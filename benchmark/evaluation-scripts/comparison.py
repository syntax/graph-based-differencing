import subprocess
import json


## !!!!!!!!!!!!!
## gumtree related functions

def run_gumtree_textdiff(file1, file2):
    try:
        # use java 17 using sdkman
        # subprocess.run(["source", "$HOME/.sdkman/bin/sdkman-init.sh"])
        # subprocess.run(["sdk", "use", "java", "17.0.14-tem"])

        # now gumtree
        result = subprocess.run(
            ["./gumtree", "textdiff", file1, file2, "-f", "JSON"],
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
        "deleted": [],
        "moved": [],
        "inserted": [],
        "updated": []
    }

    for action in data.get("actions", []):
        action_type = action["action"]
        tree_info = action["tree"]

        char_range = extract_char_range(tree_info)

        if action_type.startswith("delete"):
            line_range = map_char_range_to_lines(char_range, char_to_line_f1)
            changed_lines["deleted"].append(line_range)
        elif action_type.startswith("insert"):
            line_range = map_char_range_to_lines(char_range, char_to_line_f2)
            changed_lines["inserted"].append(line_range)
        elif action_type.startswith("update"):
            line_range = map_char_range_to_lines(char_range, char_to_line_f1)
            changed_lines["updated"].append(line_range)
        elif action_type.startswith("move"):
            line_range = map_char_range_to_lines(char_range, char_to_line_f1)
            changed_lines["moved"].append(line_range)

    return changed_lines



## !!!!!!!!!!!!!
## PDGdiff related functions

def parse_pdgdiff_output(output):
    data = json.loads(output)
    changed_lines = {
        "deleted": [],
        "moved": [],
        "inserted": [],
        "updated": []
    }

    for action in data.get("actions", []):
        action_type = action["action"]
        old_line = action.get("oldLine")
        new_line = action.get("newLine")

        if action_type == "Delete":
            old_line = action.get("line")
            if old_line is not None:
                changed_lines["deleted"].append(old_line)
        elif action_type == "Insert":
            new_line = action.get("line")
            if new_line is not None:
                changed_lines["inserted"].append(new_line)
        elif action_type == "Update":
            if old_line is not None:
                changed_lines["updated"].append(old_line)
            # if new_line is not None and new_line != old_line:
            #     changed_lines["updated"].append(new_line)
        elif action_type == "Move":
            if old_line is not None:
                changed_lines["moved"].append(old_line)
            # if new_line is not None and new_line != old_line:
            #     changed_lines["moved"].append(new_line)

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
    changed_lines["deleted"] = expand_and_sort(changed_lines["deleted"])
    changed_lines["inserted"] = expand_and_sort(changed_lines["inserted"])
    changed_lines["updated"] = expand_and_sort(changed_lines["updated"])
    changed_lines["moved"] = expand_and_sort(changed_lines["moved"])

    return changed_lines

def total_number_changes_lines(changed_lines):
    total = 0
    for change_type, lines in changed_lines.items():
        total += len(lines)
    return total


def total_number_changes_lines_excluding_mv(changed_lines):
    total = 0
    for change_type, lines in changed_lines.items():
        if change_type != "moved":
            total += len(lines)
    return total



def main():
    # set appropriately, todo eventually iterature through files by file
    file1 = "TestAdder1.java"
    file2 = "TestAdder2.java"

    json_output = run_gumtree_textdiff(file1, file2)
    if not json_output:
        print("Failed to generate GumTree diff.")
        return

    char_to_line_f1 = map_char_to_line(file1)
    char_to_line_f2 = map_char_to_line(file2)

    gt_changes = parse_gumtree_json(json_output, char_to_line_f1, char_to_line_f2)
    gt_changes = handle_changed_lines(gt_changes)

    print("---- Changed Lines report for gumtree:")
    for change_type, lines in gt_changes.items():
        print(f"{change_type.capitalize()}: {lines}")
    print(f"!! > Total number of changed lines: {total_number_changes_lines(gt_changes)}")
    print(f"!! > Total number of changed lines excluding moved: {total_number_changes_lines_excluding_mv(gt_changes)}")

    # pdgdiff
    path = "../../soot-pdg/out/diff.json"
    pdg_changes = parse_pdgdiff_output(open(path).read())
    pdg_changes = handle_changed_lines(pdg_changes)

    print("---- Changed Lines report for pdgdiff:")
    for change_type, lines in pdg_changes.items():
        print(f"{change_type.capitalize()}: {lines}")
    print(f"!! > Total number of changed lines: {total_number_changes_lines(pdg_changes)}")
    print(f"!! > Total number of changed lines excluding moved: {total_number_changes_lines_excluding_mv(pdg_changes)}")



if __name__ == "__main__":
    main()
