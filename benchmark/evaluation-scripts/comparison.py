import subprocess
import json
import os


# CONSTANTS, DEFINE

GUMTREE_PATH = "../../../gumtree-4.0.0-beta2 2/bin"
PDG_OUT_PATH = "../../out/diff.json"

## !!!!!!!!!!!!!
## gumtree related functions

def run_gumtree_textdiff(file1, file2):
    try:
        # use java 17 using sdkman
        # subprocess.run(["source", "$HOME/.sdkman/bin/sdkman-init.sh"])
        # subprocess.run(["sdk", "use", "java", "17.0.14-tem"])

        # now gumtree
        result = subprocess.run(
            [f"./{GUMTREE_PATH}/gumtree", "textdiff", file1, file2, "-f", "JSON"],
            capture_output=True,
            text=True
        )
        result.check_returncode()
        print(result.stdout)
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Error running GumTree: {e.stderr}")
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
    return sum(len(lines) for key, lines in changed_lines.items() if key != "moved_src")

def report_changed_lines(changed_lines, approach_name):
    print(f"---- Changed Lines report for {approach_name}:")
    for change_type, lines in changed_lines.items():
        print(f"{change_type.capitalize()}: {lines}") 
    # for PDGDiff, dont include moves, but for Gumtree include moves   
    if approach_name == "PDGdiff":
        print(f" !!! !! > Total number of changed lines in source: {len(set(changed_lines['deleted_src'] + changed_lines['updated_src']))}")
        print(f" !!! !! > Total number of changed lines in destination (not incl moves): {len(set(changed_lines['inserted_dst'] + changed_lines['updated_dst']))}")
        print(f"!! > Total number of changed lines overall: {total_number_changes_lines(changed_lines)}")
        print(f"!! > Total number of changed lines excluding moved_src: {total_number_changes_lines_excluding_mv(changed_lines)}")
    else:  
        print(f" !!! !! > Total number of changed lines in source: {len(set(changed_lines['deleted_src'] + changed_lines['updated_src'] + changed_lines['moved_src']))}")
        print(f" !!! !! > Total number of changed lines in destination: {len(set(changed_lines['inserted_dst'] + changed_lines['updated_dst'] + changed_lines['moved_dst']))}")
        print(f"!! > Total number of changed lines overall: {total_number_changes_lines(changed_lines)}")
        print(f"!! > Total number of changed lines excluding moved_src: {total_number_changes_lines_excluding_mv(changed_lines)}")
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


def main():

    files_to_differece, commits = crawl_datasets()
    print(f"[notif] successfully managed to gather complete details on {len(files_to_differece)} pairs of files.")
    print(f"[notif] this comes from {commits} pairs of commits (before/after).")
    with open("dataset_info.json", "w") as f:
        json.dump(files_to_differece, f, indent=4)


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
