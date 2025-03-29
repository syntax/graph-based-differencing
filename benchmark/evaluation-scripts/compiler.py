import os
import subprocess
import shutil
from pathlib import Path
from typing import Optional

# change to "verbose" for detailed logs, or "stats" for minimal logs
OUTPUT_MODE = "stats"

GH_JAVA_PROJECTS = {
    # 'apache-commons-cli': 'https://github.com/apache/commons-cli.git', # not working
    'google-guava': 'https://github.com/google/guava.git', # working ✅
    'ok-http': 'https://github.com/square/okhttp.git', # working ✅
    # 'h2': 'https://github.com/h2database/h2database.git', # not working
    # 'zaproxy': 'https://github.com/zaproxy/zaproxy.git', # too many deprecated dependencies
    # 'jabref': 'https://github.com/JabRef/jabref.git',
    # 'elastic-search': 'https://github.com/elastic/elasticsearch.git', # not working, needs older version of gradle
    # 'killbill': 'https://github.com/killbill/killbill.git', #  not working, some dependencies are not avaialble anymore
    # 'drool': 'https://github.com/kiegroup/drools.git', # not working, old Drools used a top-level aggregator that was removed, or the parent was never published to Maven Central.
    'signal-server': 'https://github.com/signalapp/Signal-Server.git', # working ✅
}

BASE_DIR = Path(__file__).parent
PROJECTS_DIR = BASE_DIR / 'projects'
BEFORE_DIR = BASE_DIR / 'before'
AFTER_DIR = BASE_DIR / 'after'

files_gathered = 0
build_stats = {
    project_name: {'passed': 0, 'failed': 0}
    for project_name in GH_JAVA_PROJECTS.keys()
}

def log(msg: str):
    """Log function that only prints if in verbose mode."""
    if OUTPUT_MODE == "verbose":
        print(msg)

def stats_log_project(project_name):
    """ Print a one-line live count for a project (only in 'stats' mode). """
    if OUTPUT_MODE == "stats":
        p = build_stats[project_name]['passed']
        f = build_stats[project_name]['failed']
        print(f"[stats] {project_name}: passed={p}, failed={f}, files={files_gathered}")

def run_cmd(cmd, cwd=None, capture_output=False):
    """
    Helper function that runs a command with optional suppression of stdout/stderr
    for 'stats' mode.
    """
    if OUTPUT_MODE == "stats" and not capture_output:
        # hide stdout/stderr in 'stats' mode by default
        return subprocess.run(
            cmd, cwd=cwd, check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )
    elif capture_output:
        return subprocess.run(
            cmd, cwd=cwd, check=True,
            capture_output=True, text=True
        )
    else:
        return subprocess.run(cmd, cwd=cwd, check=True)

def clone_repos():
    PROJECTS_DIR.mkdir(exist_ok=True)
    for project_name, repo_url in GH_JAVA_PROJECTS.items():
        project_path = PROJECTS_DIR / project_name
        if not project_path.exists():
            log(f"Cloning {repo_url} into {project_path}")
            run_cmd(['git', 'clone', repo_url, str(project_path)], cwd=BASE_DIR)

def checkout_commit(repo_path, commit_hash):
    #     force checkout a specific commit, discarding any local changes,
    #     and clean untracked files to ensure a fresh state.
    try:
        run_cmd(['git', 'checkout', '-f', commit_hash], cwd=repo_path)
        run_cmd(['git', 'clean', '-xfd'], cwd=repo_path)
    except subprocess.CalledProcessError as e:
        log(f"Error during checkout of commit {commit_hash}: {e}")
        raise  # re-raise so can catch it at a higher level if needed

def get_previous_commit(repo_path, commit_hash):
    result = run_cmd(['git', 'rev-parse', f'{commit_hash}^'],
                     cwd=repo_path,
                     capture_output=True)
    return result.stdout.strip() # result hash

def process_commits():
    global files_gathered

    for project_name in GH_JAVA_PROJECTS.keys():
        repo_path = PROJECTS_DIR / project_name
        if project_name == 'h2': repo_path = repo_path / 'h2'
        if project_name == 'drool': repo_path = repo_path / 'drools-core'
        if not repo_path.exists():
            log(f"Repository for {project_name} does not exist. Skipping.")
            continue

        successfully_compiled_commits = set()

        for stage, stage_dir in [('before', BEFORE_DIR), ('after', AFTER_DIR)]:
            project_stage_dir = stage_dir / project_name
            if not project_stage_dir.exists():
                log(f"Stage directory {project_stage_dir} does not exist. Skipping.")
                continue

            for commit_dir in project_stage_dir.iterdir():
                if commit_dir.is_dir():
                    commit_hash = commit_dir.name
                    target_commit = None

                    if stage == 'before':
                        try:
                            target_commit = get_previous_commit(repo_path, commit_hash)
                        except subprocess.CalledProcessError:
                            log(f"Cannot determine previous commit for {commit_hash}. Probably the first commit.")
                            continue
                    else:
                        target_commit = commit_hash

                    if not target_commit:
                        continue

                    log(f"Processing {stage} commit {target_commit} for {project_name}")
                    try:
                        checkout_commit(repo_path, target_commit)
                    except subprocess.CalledProcessError:
                        # mark as failed if we can't check it out
                        build_stats[project_name]['failed'] += 1
                        stats_log_project(project_name)
                        continue

                    success = compile_project(repo_path, commit_dir)
                    if success:
                        build_stats[project_name]['passed'] += 1
                        successfully_compiled_commits.add(commit_hash)
                    else:
                        build_stats[project_name]['failed'] += 1

                    stats_log_project(project_name)

        # incr `files_gathered` only if both 'before' and 'after' succeed
        for commit_hash in successfully_compiled_commits:
            if (BEFORE_DIR / project_name / commit_hash).exists() and \
               (AFTER_DIR / project_name / commit_hash).exists():
                files_gathered += len(list((BEFORE_DIR / project_name / commit_hash).rglob('*.java')))

    # final stats log
    print(f"Files gathered: {files_gathered}")



def find_build_xml(repo_path: Path) -> Optional[Path]:
    possible_paths = [
        repo_path / 'build.xml',
        repo_path / 'build' / 'build.xml',
    ]
    for candidate in possible_paths:
        if candidate.exists():
            return candidate
    return None



def compile_project(repo_path, commit_dir) -> bool:
    """
    This works in a decent amount of cases, but not every case.
    Compile the project using:
      1) Gradle wrapper if available (unless it's an old codehaus version)
      2) System Gradle if build.gradle found
      3) Maven (pom.xml)
      4) Ant (build.xml)
      5) Manual fallback
    """
    gradlew_path = repo_path / 'gradlew'
    gradle_wrapper_props = repo_path / 'gradle' / 'wrapper' / 'gradle-wrapper.properties'
    build_gradle = repo_path / 'build.gradle'
    pom_xml = repo_path / 'pom.xml'
    build_xml = find_build_xml(repo_path)


    try:
        # 1) Gradle wrapper (check if it's referencing codehaus.org)
        if gradlew_path.exists() and gradle_wrapper_props.exists():
            with open(gradle_wrapper_props, 'r', encoding='utf-8') as f:
                contents = f.read()
            if 'dist.codehaus.org' in contents:
                log(f"Old Gradle wrapper references codehaus.org; skipping wrapper for {repo_path}")
            else:
                log(f">> Using Gradle Wrapper to build {repo_path}")
                run_cmd([str(gradlew_path), 'clean', 'build', '-x', 'test', '--stacktrace'], cwd=repo_path)
                copy_all_gradle_compiled_files(repo_path, commit_dir)
                return True

        # 2) System Gradle
        if build_gradle.exists():
            log(f">> Using system Gradle to build {repo_path}")
            run_cmd(['gradle', 'clean', 'build', '-x', 'test', '--stacktrace'], cwd=repo_path)
            copy_all_gradle_compiled_files(repo_path, commit_dir)
            return True

        # 3) Maven
        if pom_xml.exists():
            log(f">> Using Maven (pom.xml) to build {repo_path}")
            run_cmd(
                    [
                        "mvn",
                        "clean",
                        "install",
                        # "-Dmaven.dependency.fallback=true",
                        "-DskipTests",
                        "-Dgpg.skip=true",
                        # "-Dmaven.wagon.http.allow=true",
                        # "-DallowInsecureProtocol=true",
                        "-B",
                    ],
                    cwd=repo_path,
                )
            copy_all_maven_compiled_files(repo_path, commit_dir)
            return True

        # 4) Ant
        if build_xml.exists():
            ant_path = shutil.which('ant')
            if ant_path is None:
                log("Ant build file found, but 'ant' is not installed. Skipping Ant build => fallback manual.")
            else:
                ant_dir = build_xml.parent
                log(f">> Using Ant to build {repo_path}")
                run_cmd(['ant', 'clean', 'compile'], cwd=ant_dir)
                copy_all_ant_compiled_files(repo_path, commit_dir)
                return True

        # 5) Manual fallback, basically never works. lol
        log(f">> No recognized or viable build system in {repo_path}. Attempting manual compilation...")
        compile_java_manually(repo_path, commit_dir)
        return True

    except subprocess.CalledProcessError as e:
        log(f"Build failed for {repo_path}. Error: {e}")
        return False


    except subprocess.CalledProcessError as e:
        log(f"Build failed for {repo_path}. Error: {e}")
        return False


    except subprocess.CalledProcessError as e:
        log(f"Build failed for {repo_path}. Error: {e}")
        return False

def copy_all_gradle_compiled_files(repo_path: Path, commit_dir: Path):
    compiled_output_dir = commit_dir / 'compiled'
    if compiled_output_dir.exists():
        shutil.rmtree(compiled_output_dir)
    compiled_output_dir.mkdir(parents=True, exist_ok=True)

    # look for all 'build/**/classes' directories
    for build_dir in repo_path.rglob('build'):
        for classes_dir in build_dir.rglob('classes'):
            if classes_dir.is_dir():
                copy_directory_contents(classes_dir, compiled_output_dir)

def copy_all_maven_compiled_files(repo_path: Path, commit_dir: Path):
    compiled_output_dir = commit_dir / 'compiled'
    if compiled_output_dir.exists():
        shutil.rmtree(compiled_output_dir)
    compiled_output_dir.mkdir(parents=True, exist_ok=True)

    # for multi-module search all submodules
    for target_dir in repo_path.rglob('target'):
        for classes_dir in target_dir.rglob('classes'):
            if classes_dir.is_dir():
                copy_directory_contents(classes_dir, compiled_output_dir)

def copy_all_ant_compiled_files(repo_path: Path, commit_dir: Path):
    compiled_output_dir = commit_dir / 'compiled'
    if compiled_output_dir.exists():
        shutil.rmtree(compiled_output_dir)
    compiled_output_dir.mkdir(parents=True, exist_ok=True)

    # the default for many Ant scripts is "build/classes" i have found
    ant_build_classes = repo_path / 'build' / 'classes'
    if ant_build_classes.exists():
        copy_directory_contents(ant_build_classes, compiled_output_dir)
    else:
        log(f"No 'build/classes' directory found; the Ant script may store classes elsewhere.")

def copy_directory_contents(src_dir: Path, dest_dir: Path):
    for item in src_dir.rglob('*'):
        if item.is_file():
            relative = item.relative_to(src_dir)
            target_file = dest_dir / relative
            target_file.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(item, target_file)

def find_src_directory(repo_path):
    possible_paths = [
        repo_path / 'h2' / 'src',
        repo_path / 'drools-core' / 'src',
        repo_path / 'src',
    ]
    for path in possible_paths:
        if path.exists():
            return path

    log(f"No 'src' directory found in {repo_path}")
    return None

def compile_java_manually(repo_path, commit_dir):
    """
    Compile the entire project manually by invoking javac on all .java files
    in 'src', then copy ALL .class files to commit_dir/compiled.
    Often fails if the project depends on external jars (e.g., JUnit). :(
    """
    src_dir = find_src_directory(repo_path)
    if not src_dir:
        log("Manual compilation aborted: no recognized src directory.")
        return

    output_dir = repo_path / 'compiled'
    if output_dir.exists():
        shutil.rmtree(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    log(f"Compiling the entire project manually into {output_dir}...")
    javac_cmd = [
        'javac',
        '-d', str(output_dir),
        '-sourcepath', str(src_dir)
    ] + [str(f) for f in src_dir.rglob("*.java")]

    run_cmd(javac_cmd, cwd=repo_path)  # may raise CalledProcessError

    target_output_dir = commit_dir / 'compiled'
    if target_output_dir.exists():
        shutil.rmtree(target_output_dir)
    target_output_dir.mkdir(parents=True, exist_ok=True)

    for item in output_dir.rglob("*"):
        if item.is_file():
            relative_path = item.relative_to(output_dir)
            target_file = target_output_dir / relative_path
            target_file.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy(item, target_file)
            log(f"Copied {item} to {target_file}")

    shutil.rmtree(output_dir, ignore_errors=True)
    log(f"Manual compilation and copying completed for {repo_path}.")

if __name__ == "__main__":
    try:
        print("[notif] Cloning repositories...")
        clone_repos()
        print("[notif] Processing commits...")
        process_commits()

        # final summary
        print("\n=== PER-PROJECT BUILD SUMMARY ===")
        for project_name, stats in build_stats.items():
            print(f"{project_name}: passed={stats['passed']}, failed={stats['failed']}")

        grand_passed = sum(s['passed'] for s in build_stats.values())
        grand_failed = sum(s['failed'] for s in build_stats.values())
        print(f"\n final total => passed={grand_passed}, failed={grand_failed}\n")

    except subprocess.CalledProcessError as e:
        if OUTPUT_MODE == "verbose":
            print(f"Error during execution: {e}")

        else:
            print("\nA build error occurred. Rerun with OUTPUT_MODE='verbose' for details.")

        print("\n=== PARTIAL BUILD SUMMARY ===")
        for project_name, stats in build_stats.items():
            print(f"{project_name}: passed={stats['passed']}, failed={stats['failed']}")
