from flask import Flask, render_template
import json
import os
from difflib import SequenceMatcher
import re

app = Flask(__name__)

TEST_CLASSES_PATH = "./testclasses"
OUT_PATH = "./out"

def read_file(filepath):
    with open(filepath, 'r') as file:
        return file.read()

def generate_color_pairs(n):
    base_colors = [
        ("highlight-change1-old", "highlight-change1-old"),
        ("highlight-change2-old", "highlight-change2-old"),
        ("highlight-change3-old", "highlight-change3-old"),
        ("highlight-change4-old", "highlight-change4-old"),
        ("highlight-change5-old", "highlight-change5-old"),
        ("highlight-change6-old", "highlight-change6-old"),
        ("highlight-change1-new", "highlight-change1-new"),
        ("highlight-change2-new", "highlight-change2-new"),
        ("highlight-change3-new", "highlight-change3-new"),
        ("highlight-change4-new", "highlight-change4-new"),
        ("highlight-change5-new", "highlight-change5-new"),
        ("highlight-change6-new", "highlight-change6-new"),
    ]
    
    color_pairs = []
    for i in range(n):
        color_pairs.append(base_colors[i % len(base_colors)])
    
    return color_pairs

def highlight_word_differences_with_colors(old_line, new_line, color_pair):
    # posibly investigate wierd spacing behaviour on front end output, think this is because of the way the split is done
    seperators = r'([.,(){};=+-/*])'
    old_words = re.split(seperators, old_line)
    new_words = re.split(seperators, new_line)
    
    old_words = [word for word in old_words if word.strip() != '']
    new_words = [word for word in new_words if word.strip() != '']
    
    old_highlight = []
    new_highlight = []

    matcher = SequenceMatcher(None, old_words, new_words)

    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == 'equal':
            old_highlight.append(' '.join(old_words[i1:i2]))
            new_highlight.append(' '.join(new_words[j1:j2]))
        elif tag == 'replace':
            old_highlight.append(f"<span class='{color_pair[0]}'>{' '.join(old_words[i1:i2])}</span>")
            new_highlight.append(f"<span class='{color_pair[1]}'>{' '.join(new_words[j1:j2])}</span>")
        elif tag == 'delete':
            old_highlight.append(f"<span class='{color_pair[0]}'>{' '.join(old_words[i1:i2])}</span>")
        elif tag == 'insert':
            new_highlight.append(f"<span class='{color_pair[1]}'>{' '.join(new_words[j1:j2])}</span>")

    return ' '.join(old_highlight), ' '.join(new_highlight)

@app.route('/')
def diff_view():
    class1_content = read_file(os.path.join(TEST_CLASSES_PATH, 'TestAdder1.java')).splitlines()
    class2_content = read_file(os.path.join(TEST_CLASSES_PATH, 'TestAdder2.java')).splitlines()

    with open(os.path.join(OUT_PATH, 'diff.json'), 'r') as diff_file:
        diff_data = json.load(diff_file)

    num_actions = len(diff_data["actions"])
    color_pairs = generate_color_pairs(num_actions)

    highlighted_class1 = class1_content[:]
    highlighted_class2 = class2_content[:]

    highlighted_diffs = []

    # todo, as of right now im not even capturing all these actions, or showin them in the output
    for i, action in enumerate(diff_data["actions"]):
        if action["action"] == "Update":
            old_line_number = action["oldLine"] - 1
            new_line_number = action["newLine"] - 1

            if 0 <= old_line_number < len(highlighted_class1) and 0 <= new_line_number < len(highlighted_class2):
                highlighted_old, highlighted_new = highlight_word_differences_with_colors(
                    class1_content[old_line_number], class2_content[new_line_number], color_pairs[i % len(color_pairs)]
                )
                highlighted_class1[old_line_number] = highlighted_old
                highlighted_class2[new_line_number] = highlighted_new
            
                highlighted_diffs.append({
                    "oldLine": action["oldLine"],
                    "newLine": action["newLine"],
                    "oldCode": highlighted_old,
                    "newCode": highlighted_new
                })
        elif action["action"] == "Insert":

            new_line_number = action["line"] - 1
  
            if 0 <= new_line_number < len(highlighted_class2):
                highlighted_new = f"<span class='{color_pairs[i % len(color_pairs)][1]}'>{class2_content[new_line_number]}</span>"
                highlighted_class2[new_line_number] = highlighted_new

                highlighted_diffs.append({
                    "oldLine": None,
                    "newLine": action["line"],

                    "oldCode": "",
                    "newCode": highlighted_new
                })

    highlighted_class1_content = '\n'.join(highlighted_class1)
    highlighted_class2_content = '\n'.join(highlighted_class2)

    return render_template('index.html', class1=highlighted_class1_content, class2=highlighted_class2_content, diffs=highlighted_diffs)

if __name__ == '__main__':
    app.run(debug=True)
