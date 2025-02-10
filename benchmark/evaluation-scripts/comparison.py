import subprocess
import json
import os
import csv
from datetime import datetime

os.system("")




# CONSTANTS, DEFINE

GUMTREE_PATH = "../../../gumtree-4.0.0-beta2 2/bin"
PDG_OUT_PATH = "out/diff.json"
CSV_LOG_FILE = "diff_results.csv"


class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    

## !!!!!!!!!!!!!
## gumtree related functions

def run_gumtree_textdiff(file1, file2):
    try:
        # use java 17 using sdkman
        # subprocess.run(["source", "$HOME/.sdkman/bin/sdkman-init.sh"])
        # subprocess.run(["sdk", "use", "java", "17.0.14-tem"])

        # now gumtree
        print(f"{bcolors.OKBLUE}[notif]{bcolors.ENDC} running GumTree.")
        result = subprocess.run(
            [f"./{GUMTREE_PATH}/gumtree", "textdiff", file1, file2, "-f", "JSON"],
            capture_output=True,
            text=True,
        )
        result.check_returncode()
        # print(result.stdout)
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"{bcolors.FAIL}[error]{bcolors.ENDC} err running GumTree: {e.stderr}")
        return None

def map_char_to_line(file_path):
    #create a mapping from character positions to line numbers.
    
    char_to_line = {}
    with open(file_path, "r") as f:
        current_pos = 0
        for line_num, line in enumerate(f, start=1):
            for _ in line:
                char_to_line[current_pos] = line_num
                current_pos += 1
    return char_to_line

def map_char_range_to_lines(char_range, char_to_line):
   # map a character range (start, end) to the corresponding line range
    #ret a single integer for single-line changes or a tuple for ranges
    if char_range:
        start_char, end_char = char_range
        start_line = char_to_line.get(start_char, None)
        end_line = char_to_line.get(end_char - 1, None)
        
        if start_line == end_line:
            return start_line
        return (start_line, end_line)
    return None

def extract_char_range(tree_info):
    # Extract character range from tree information string eg: "identifier: print [19,24]" -> (19, 24)
    try:
        start_idx = tree_info.index("[")
        end_idx = tree_info.index("]")
        char_range = tree_info[start_idx + 1 : end_idx]
        start_char, end_char = map(int, char_range.split(","))
        return (start_char, end_char)
    except (ValueError, IndexError):
        return None

def parse_gumtree_json(json_output, char_to_line_f1, char_to_line_f2):
    #parse the GumTree JSON output and extract changed lines.

    data = json.loads(json_output)
    changed_lines = {
        "deleted_src": [],
        "moved_src": [],
        "moved_dst": [],
        "inserted_dst": [],
        "updated_src": [],
        "updated_dst": []
    }
    
    src_dest_map = {}
    for match in data.get("matches", []):
        src_range = extract_char_range(match["src"])
        dest_range = extract_char_range(match["dest"])
        if src_range and dest_range:
            src_dest_map[src_range] = dest_range

    for action in data.get("actions", []):
        action_type = action["action"]
        tree_info = action["tree"]
        
        char_range = extract_char_range(tree_info)
        
        if action_type.startswith("delete"):
            line_range = map_char_range_to_lines(char_range, char_to_line_f1)
            changed_lines["deleted_src"].append(line_range)
        elif action_type.startswith("insert"):
            line_range = map_char_range_to_lines(char_range, char_to_line_f2)
            changed_lines["inserted_dst"].append(line_range)
        elif action_type.startswith("update"):
            # Get source lines
            src_line_range = map_char_range_to_lines(char_range, char_to_line_f1)
            changed_lines["updated_src"].append(src_line_range)
            
            # Find corresponding destination range from matches
            dest_char_range = src_dest_map.get(char_range, None)
            dest_line_range = map_char_range_to_lines(dest_char_range, char_to_line_f2)
            if dest_line_range:
                changed_lines["updated_dst"].append(dest_line_range)
        elif action_type.startswith("move"):
            # i can ony get before infromation from the move for some reason..
            line_range = map_char_range_to_lines(char_range, char_to_line_f1)
            changed_lines["moved_src"].append(line_range)

            dest_char_range = src_dest_map.get(char_range, None)
            dest_line_range = map_char_range_to_lines(dest_char_range, char_to_line_f2)
            if dest_line_range:
                changed_lines["moved_dst"].append(dest_line_range)
    
    return changed_lines



## !!!!!!!!!!!!!
## PDGdiff related functions



# java -jar target/soot-pdg-1.0-SNAPSHOT.jar ./src/main/java/org/pdgdiff/testclasses/TestAdder1.java ./src/main/java/org/pdgdiff/testclasses/TestAdder2.java ./target/classes ./target/classes org.pdgdiff.testclasses.TestAdder1 org.pdgdiff.testclasses.TestAdder2 GED

def run_pdg_textdiff(before_src_dir, after_src_dir, before_compiled_dir, after_compiled_dir, before_class_fullyqualified, after_class_fullyqualified, strategy):
    if strategy not in ["vf2", "ged","ullmann"]:
        print(f"{bcolors.FAIL}[error]{bcolors.ENDC} invalid strategy: {strategy}")
        return None
    try:
        cmd = ["java", "-jar", "../../target/soot-pdg-1.0-SNAPSHOT.jar", before_src_dir, after_src_dir, before_compiled_dir, after_compiled_dir, before_class_fullyqualified, after_class_fullyqualified, strategy]
        print(f"{bcolors.OKBLUE}[notif]{bcolors.ENDC} running PDGDiff with command: {' '.join(cmd)}")
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=90
        )
        result.check_returncode()
        return result.stdout
    except subprocess.TimeoutExpired:
        print(f"{bcolors.WARNING}[warning]{bcolors.ENDC} PDGDiff took too long (>90s) and was skipped.")
        return None
    except subprocess.CalledProcessError as e:
        print(f"{bcolors.FAIL}[error]{bcolors.ENDC} error running PDGDiff: {e.stderr}")
        return None

def parse_pdgdiff_output(output):
    data = json.loads(output)
    changed_lines = {
        "deleted_src": [],
        "moved_src": [],
        "moved_dst": [],
        "inserted_dst": [],
        "updated_src": [],
        "updated_dst": []
    }

    for action in data.get("actions", []):
        action_type = action["action"]
        old_line = action.get("oldLine")
        new_line = action.get("newLine")

        if action_type == "Delete":
            old_line = action.get("line")
            if old_line is not None:
                changed_lines["deleted_src"].append(old_line)
        elif action_type == "Insert":
            new_line = action.get("line")
            if new_line is not None:
                changed_lines["inserted_dst"].append(new_line)
        elif action_type == "Update":
            if old_line is not None:
                changed_lines["updated_src"].append(old_line)
                changed_lines["updated_dst"].append(new_line)
            # if new_line is not None and new_line != old_line:
            #     changed_lines["updated"].append(new_line)
        elif action_type == "Move":
            if old_line is not None:
                changed_lines["moved_src"].append(old_line)
                changed_lines["moved_dst"].append(new_line)
            # if new_line is not None and new_line != old_line:
            #     changed_lines["moved_src"].append(new_line)

    return changed_lines


    


# !!!!!!
# generic functions

def sort_key(item):
    if isinstance(item, tuple):
        return min(item)
    return item


def handle_changed_lines(changed_lines):
    # Function to expand tuples into lists of line numbers
    def expand_and_sort(lines):
        expanded = []
        for line in lines:
            if isinstance(line, tuple):
                expanded.extend(range(line[0], line[1] + 1))  # Expand tuple to range
            else:
                expanded.append(line)
        return sorted(set(expanded))  # Sort and remove duplicates

    # Expand and process each type of change
    changed_lines["deleted_src"] = expand_and_sort(changed_lines["deleted_src"])
    changed_lines["inserted_dst"] = expand_and_sort(changed_lines["inserted_dst"])
    changed_lines["updated_src"] = expand_and_sort(changed_lines["updated_src"])
    changed_lines["updated_dst"] = expand_and_sort(changed_lines["updated_dst"])
    changed_lines["moved_src"] = expand_and_sort(changed_lines["moved_src"])
    changed_lines["moved_dst"] = expand_and_sort(changed_lines["moved_dst"])

    return changed_lines

def total_number_changes_lines(changed_lines):
    return sum(len(lines) for lines in changed_lines.values())


def total_number_changes_lines_excluding_mv(changed_lines):
    return sum(len(lines) for key, lines in changed_lines.items() if key != "moved_src" and key != "moved_dst") 

def report_changed_lines(changed_lines, approach_name):
    print(f"---- Changed Lines report for {approach_name}:")
    for change_type, lines in changed_lines.items():
        print(f"{change_type.capitalize()}: {lines}") 
    # for PDGDiff, dont include moves, but for Gumtree include moves   
    if approach_name == "PDGdiff":
        print(f" !!! !! > Total number of changed lines in source: {len(set(changed_lines['deleted_src'] + changed_lines['updated_src']))}")
        print(f" !!! !! > Total number of changed lines in destination (not incl moves): {len(set(changed_lines['inserted_dst'] + changed_lines['updated_dst']))}")
        print(f"!! > Total number of changed lines overall: {total_number_changes_lines(changed_lines)}")
        print(f"!! > Total number of changed lines excluding moved: {total_number_changes_lines_excluding_mv(changed_lines)}")
    else:  
        print(f" !!! !! > Total number of changed lines in source: {len(set(changed_lines['deleted_src'] + changed_lines['updated_src'] + changed_lines['moved_src']))}")
        print(f" !!! !! > Total number of changed lines in destination: {len(set(changed_lines['inserted_dst'] + changed_lines['updated_dst'] + changed_lines['moved_dst']))}")
        print(f"!! > Total number of changed lines overall: {total_number_changes_lines(changed_lines)}")
        print(f"!! > Total number of changed lines excluding moved: {total_number_changes_lines_excluding_mv(changed_lines)}")
        print("\n")


# !!!!!! Functions for comparing the two approaches

def find_compiled_class(compiled_dir, source_file):
    """
    steps
      1. get the base name of the source file (e.g. 'StringUtils' from 'StringUtils.java').
      2. recursively walk the compiled_dir to find a file with the name 'StringUtils.class'.
      3. if found, compute its relative path with respect to compiled_dir, replace path separators
         with dots, and return the fully qualified name.
    
    not found? , returns None.
    """
    base_name = os.path.splitext(os.path.basename(source_file))[0]
    for root, _, files in os.walk(compiled_dir):
        for f in files:
            if f == base_name + ".class":
                # get the relative directory path from the compiled root
                rel_dir = os.path.relpath(root, compiled_dir)
                if rel_dir == ".":
                    return base_name
                else:
                    # convert directory structure to dot notation.
                    fqcn = rel_dir.replace(os.sep, ".") + "." + base_name
                    return fqcn
    return None


def crawl_datasets():
    projects = ["google-guava", "ok-http", "signal-server"]
    base_path = "../datasets/gh-java"
    before_path = os.path.join(base_path, "before")
    after_path = os.path.join(base_path, "after")
    
    dataset_info = []
    good_commits = 0


    for project in projects:
        # print(f">>> Project: {project}")

        project_before_path = os.path.join(before_path, project)
        project_after_path = os.path.join(after_path, project)

        for commit_id in os.listdir(project_before_path):
            commit_good = True
            # print(f"> Processing commit: {commit_id}")

            # compiled paths, to be returned
            before_compiled_path = os.path.join(project_before_path, commit_id, "compiled")
            after_compiled_path = os.path.join(project_after_path, commit_id, "compiled")

            before_commit_path = os.path.join(project_before_path, commit_id)
            after_commit_path = os.path.join(project_after_path, commit_id)

            for changed_file in os.listdir(before_commit_path):
                if changed_file == "compiled" or not changed_file.endswith(".java"): continue
                # print(f"Processing file: {changed_file}")

                # to be returned
                before_file = os.path.join(before_commit_path, changed_file)
                after_file = os.path.join(after_commit_path, changed_file)

                before_class_fqcn = find_compiled_class(before_compiled_path, changed_file)
                after_class_fqcn = find_compiled_class(after_compiled_path, changed_file)


                file_info = {
                    "project": project,
                    "commit_id": commit_id,
                    "changed_file": changed_file,
                    "before_compiled_dir": before_compiled_path,
                    "after_compiled_dir": after_compiled_path,
                    "before_file_dir": before_file,
                    "after_file_dir": after_file,
                    "before_class_fullyqualified": before_class_fqcn,
                    "after_class_fullyqualified": after_class_fqcn,
                }


                # some files just do not manage to compile, for whatever reason, this is rare on the subset of projects
                # but im going to 
                if before_class_fqcn and after_class_fqcn:
                    dataset_info.append(file_info)
                else:
                    commit_good = False
                
            if commit_good:
                good_commits += 1

    return dataset_info, good_commits

def report_changed_lines_brief(changed_lines, approach_name, file_name, total_excluding_moves_and_comments, total_excluding_non_soot):
    # Compute total changed lines excluding moves
    total_changes = total_number_changes_lines(changed_lines)
    total_excluding_moves = total_number_changes_lines_excluding_mv(changed_lines)

    color = bcolors.OKGREEN

    print(color + f"[{approach_name}] {file_name}: {bcolors.BOLD}{total_excluding_non_soot} lines that are repr by the graph.{bcolors.ENDC} (excl comments + imports + moves: {total_excluding_moves_and_comments}, excl moves: {total_excluding_moves}, abs total: {total_changes})")


def initialize_csv():
    if not os.path.exists(CSV_LOG_FILE):
        with open(CSV_LOG_FILE, mode="w", newline="") as file:
            writer = csv.writer(file)
            writer.writerow([
                "Timestamp", "Project", "Commit ID", "Changed File", "Approach",
                "Total Changed Lines", "Total Changed Lines (Excl. Moves)",
                "Total Changed Lines (Excl. Moves AND Comments)",
                "Total Changed Lines (Excl. everything non soot)"
                "Deleted Lines (Src)", "Inserted Lines (Dst)",
                "Updated Lines (Src)", "Updated Lines (Dst)",
                "Moved Lines (Src)", "Moved Lines (Dst)", "Errors"
            ])

def log_results_to_csv(file_info, approach_name, changed_lines, error_msg="",non_comment_changed_count=None, non_soot_changed_count=None):
    try:
        with open(CSV_LOG_FILE, mode="a", newline="") as file:
            writer = csv.writer(file)
            writer.writerow([
                datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                file_info["project"],
                file_info["commit_id"],
                file_info["changed_file"],
                approach_name,
                total_number_changes_lines(changed_lines),
                total_number_changes_lines_excluding_mv(changed_lines),
                non_comment_changed_count if non_comment_changed_count is not None else 0,
                non_soot_changed_count if non_soot_changed_count is not None else 0,
                str(changed_lines["deleted_src"]),
                str(changed_lines["inserted_dst"]),
                str(changed_lines["updated_src"]),
                str(changed_lines["updated_dst"]),
                str(changed_lines["moved_src"]),
                str(changed_lines["moved_dst"]),
                error_msg
            ])
            print(f"{bcolors.OKCYAN}[status]{bcolors.ENDC} > logged results for {file_info['changed_file']} ({approach_name}) to CSV.")
    except Exception as e:
        print(f"{bcolors.WARNING}[warning]{bcolors.ENDC} error writing to CSV: {e}")



def is_comment_or_blank(line: str) -> bool:
    stripped = line.strip()
    if not stripped:
        return True  # blank line
    # Single-line comment //...
    if stripped.startswith("//"):
        return True
    if stripped.startswith("import"):
        return True
    # Block comment lines /**, /*, or * ...
    if stripped.startswith("/*") or stripped.startswith("*") or stripped.startswith("/**"):
        return True
    return False


def is_non_soot_detectable(line: str) -> bool:
    stripped = line.strip()
    if not stripped:
        return True  # blank line
    if stripped.startswith("//"):
        return True
    if stripped.startswith("import"):
        return True
    if stripped.startswith("package"):
        return True
    
    if stripped == "}" or stripped == "};":
        return True
    if (stripped.startswith("else ") or stripped.startswith("} else")) and "if" not in stripped:
        return True
    if stripped.startswith("try"):
        return True
    if stripped.startswith("catch")or stripped.startswith("} catch"):
        return True

    if stripped.startswith("/*") or stripped.startswith("*") or stripped.startswith("/**"):
        return True
    return False


def count_changed_lines_excluding_comments(changed_lines, src_file_path, dst_file_path):
    try:
        with open(src_file_path, "r", encoding="utf-8") as f:
            src_lines = f.readlines()
    except Exception:
        src_lines = []

    try:
        with open(dst_file_path, "r", encoding="utf-8") as f:
            dst_lines = f.readlines()
    except Exception:
        dst_lines = []

    total_non_comment_changed_count = 0
    
    def count_non_comment(file_lines, line_nums):
        """Count how many lines in `file_lines` (1-based) among `line_nums` are not pure comment/blank."""
        count = 0
        for ln in line_nums:
            if 1 <= ln <= len(file_lines):
                if not is_comment_or_blank(file_lines[ln - 1]):
                    count += 1
                # else:
                #     print(f"{bcolors.WARNING}[warning]{bcolors.ENDC} line {ln} is a comment, an import or blank.\n{file_lines[ln - 1]}")
        return count

    # Changes in source file
    total_non_comment_changed_count += count_non_comment(src_lines, changed_lines["deleted_src"])
    total_non_comment_changed_count += count_non_comment(src_lines, changed_lines["updated_src"])
    # total_non_comment_changed_count += count_non_comment(src_lines, changed_lines["moved_src"])

    # Changes in destination file
    total_non_comment_changed_count += count_non_comment(dst_lines, changed_lines["inserted_dst"])
    total_non_comment_changed_count += count_non_comment(dst_lines, changed_lines["updated_dst"])
    # total_non_comment_changed_count += count_non_comment(dst_lines, changed_lines["moved_dst"])

    return total_non_comment_changed_count


def count_changed_lines_excluding_nonsoot(changed_lines, src_file_path, dst_file_path):

    try:
        with open(src_file_path, "r", encoding="utf-8") as f:
            src_lines = f.readlines()
    except Exception:
        src_lines = []

    try:
        with open(dst_file_path, "r", encoding="utf-8") as f:
            dst_lines = f.readlines()
    except Exception:
        dst_lines = []

    total_non_comment_changed_count = 0
    
    def count_non_soot(status, file_lines, line_nums):
        count = 0
        for ln in line_nums:
            if 1 <= ln <= len(file_lines):
                if not is_non_soot_detectable(file_lines[ln - 1]):
                    count += 1
                else:
                    print(f"{bcolors.WARNING}[warning]{bcolors.ENDC} line {ln} from {status} file is not captured by soot and being excluded from comparison;\n{file_lines[ln - 1]}")
        return count

    total_non_comment_changed_count += count_non_soot("src",src_lines, changed_lines["deleted_src"])
    total_non_comment_changed_count += count_non_soot("src",src_lines, changed_lines["updated_src"])
    # total_non_comment_changed_count += count_non_comment(src_lines, changed_lines["moved_src"])

    # Changes in destination file
    total_non_comment_changed_count += count_non_soot("dst",dst_lines, changed_lines["inserted_dst"])
    total_non_comment_changed_count += count_non_soot("dst",dst_lines, changed_lines["updated_dst"])
    # total_non_comment_changed_count += count_non_comment(dst_lines, changed_lines["moved_dst"])

    return total_non_comment_changed_count

def main():

    files_to_differece, commits = crawl_datasets()
    print(f"{bcolors.OKBLUE}[notif]{bcolors.ENDC} successfully managed to gather complete details on {len(files_to_differece)} pairs of files.")
    print(f"{bcolors.OKBLUE}[notif]{bcolors.ENDC} this comes from {commits} pairs of commits (before/after).")
    with open("dataset_info.json", "w") as f:
        json.dump(files_to_differece, f, indent=4)



    for file_info in files_to_differece:
        print(f"{bcolors.OKCYAN}[status]{bcolors.ENDC} > processing file: {bcolors.BOLD}{file_info['changed_file']}{bcolors.ENDC} from commit {bcolors.BOLD}{file_info['commit_id']}{bcolors.ENDC} in project {bcolors.BOLD}{file_info['project']}{bcolors.ENDC}")
        try:
            file1 = file_info["before_file_dir"]
            file2 = file_info["after_file_dir"]

            # gumtree
            json_output = run_gumtree_textdiff(file1, file2)
            if not json_output:
                print("Failed to generate GumTree diff.")
                raise Exception("Failed to generate GumTree diff.")

            char_to_line_f1 = map_char_to_line(file1)
            char_to_line_f2 = map_char_to_line(file2)

            gt_changes = parse_gumtree_json(json_output, char_to_line_f1, char_to_line_f2)
            gt_changes = handle_changed_lines(gt_changes)
            
            gt_non_comment_count = count_changed_lines_excluding_comments(gt_changes, file1, file2)
            gt_non_soot_count = count_changed_lines_excluding_nonsoot(gt_changes, file1, file2)


            
            # report_changed_lines(gt_changes, "GumTree")
            #  TODO
            # pdgdiff
            strategy = "vf2"
            run_pdg_textdiff(file_info["before_file_dir"],
                                file_info["after_file_dir"],
                                file_info["before_compiled_dir"], 
                                file_info["after_compiled_dir"], 
                                file_info["before_class_fullyqualified"], 
                                file_info["after_class_fullyqualified"], 
                                strategy=strategy)
            pdg_vf2_changes = parse_pdgdiff_output(open(PDG_OUT_PATH).read())
            pdg_vf2_changes = handle_changed_lines(pdg_vf2_changes)

            pdg_vf2_non_comment_count = count_changed_lines_excluding_comments(pdg_vf2_changes, file1, file2)
            pdg_vf2_non_soot_count = count_changed_lines_excluding_nonsoot(pdg_vf2_changes, file1, file2)
            #   (pdg_changes, "PDGdiff") 


            strategy = "ged"
            run_pdg_textdiff(file_info["before_file_dir"],
                                file_info["after_file_dir"],
                                file_info["before_compiled_dir"], 
                                file_info["after_compiled_dir"], 
                                file_info["before_class_fullyqualified"], 
                                file_info["after_class_fullyqualified"], 
                                strategy=strategy)
            pdg_ged_changes = parse_pdgdiff_output(open(PDG_OUT_PATH).read())
            pdg_ged_changes = handle_changed_lines(pdg_ged_changes)

            pdg_ged_non_comment_count = count_changed_lines_excluding_comments(pdg_ged_changes, file1, file2)
            pdg_get_non_soot_count = count_changed_lines_excluding_nonsoot(pdg_ged_changes, file1, file2)

            print(f"{bcolors.OKBLUE}[notif]{bcolors.ENDC} parsing results...")
            report_changed_lines_brief(gt_changes, "GumTree", file_info["changed_file"], gt_non_comment_count, gt_non_soot_count)
            report_changed_lines_brief(pdg_vf2_changes, "PDGdiff-vf2", file_info["changed_file"], pdg_vf2_non_comment_count, pdg_vf2_non_soot_count)
            report_changed_lines_brief(pdg_ged_changes, "PDGdiff-ged", file_info["changed_file"], pdg_ged_non_comment_count, pdg_get_non_soot_count)


            log_results_to_csv(file_info, "GumTree", gt_changes, gt_non_comment_count, gt_non_soot_count)
            log_results_to_csv(file_info, "PDGdiff-VF2", pdg_vf2_changes, pdg_vf2_changes, pdg_vf2_non_comment_count)
            log_results_to_csv(file_info, "PDGdiff-GED", pdg_ged_changes, pdg_ged_non_comment_count, pdg_get_non_soot_count)


        except Exception as e:
            print(f"{bcolors.FAIL}[error]{bcolors.ENDC} error processing file: {file_info['changed_file']}, {e}")
            log_results_to_csv(file_info, "Error", {}, e)
            continue


    #  # set appropriately, todo eventually iterature through files by file
    # file1 = GUMTREE_PATH + "/TestAdder1.java"
    # file2 = GUMTREE_PATH + "/TestAdder2.java"
    
    # # gumtree
    # json_output = run_gumtree_textdiff(file1, file2)
    # if not json_output:
    #     print("Failed to generate GumTree diff.")
    #     return

    # char_to_line_f1 = map_char_to_line(file1)
    # char_to_line_f2 = map_char_to_line(file2)

    # gt_changes = parse_gumtree_json(json_output, char_to_line_f1, char_to_line_f2)
    # gt_changes = handle_changed_lines(gt_changes)
    
    # report_changed_lines(gt_changes, "GumTree")

    # # pdgdiff
    # pdg_changes = parse_pdgdiff_output(open(PDG_OUT_PATH).read())
    # pdg_changes = handle_changed_lines(pdg_changes)


    # report_changed_lines(pdg_changes, "PDGdiff")



if __name__ == "__main__":
    main()
