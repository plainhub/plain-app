#!/usr/bin/env python3
"""
Fill missing translations in all locale files using the en-US (values) strings.xml as reference.
Missing entries are appended with the English value as a fallback.
"""

import xml.etree.ElementTree as ET
import glob
import os
import re

ET.register_namespace('', '')  # keep default namespace clean

RES_DIR = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')
EN_FILE = os.path.join(RES_DIR, 'values', 'strings.xml')


def parse_ordered(filepath):
    """Return (ordered list of (tag, name), dict of string name->text, dict of plurals name->items)"""
    tree = ET.parse(filepath)
    root = tree.getroot()
    return tree, root


def get_en_entries(root):
    strings = {}
    plurals = {}
    order = []
    for child in root:
        name = child.get('name')
        if child.tag == 'string':
            strings[name] = child
            order.append(('string', name))
        elif child.tag == 'plurals':
            plurals[name] = child
            order.append(('plurals', name))
    return strings, plurals, order


def elem_to_str(elem):
    s = ET.tostring(elem, encoding='unicode')
    return s


def fill_locale(locale_file, en_strings, en_plurals):
    tree, root = parse_ordered(locale_file)
    
    loc_strings = set()
    loc_plurals = set()
    for child in root:
        name = child.get('name')
        if child.tag == 'string':
            loc_strings.add(name)
        elif child.tag == 'plurals':
            loc_plurals.add(name)
    
    missing_strings = {k: v for k, v in en_strings.items() if k not in loc_strings}
    missing_plurals = {k: v for k, v in en_plurals.items() if k not in loc_plurals}
    
    if not missing_strings and not missing_plurals:
        return 0
    
    # Append missing entries to root
    for name, elem in missing_strings.items():
        import copy
        new_elem = copy.deepcopy(elem)
        root.append(new_elem)
    
    for name, elem in missing_plurals.items():
        import copy
        new_elem = copy.deepcopy(elem)
        root.append(new_elem)
    
    # Write back - use a custom serializer to preserve formatting
    write_xml(tree, root, locale_file, missing_strings, missing_plurals)
    return len(missing_strings) + len(missing_plurals)


def write_xml(tree, root, filepath, missing_strings, missing_plurals):
    """Re-read original file and append missing entries before </resources>"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    import copy
    
    additions = []
    for name, elem in missing_strings.items():
        new_elem = copy.deepcopy(elem)
        s = ET.tostring(new_elem, encoding='unicode')
        additions.append('    ' + s)
    
    for name, elem in missing_plurals.items():
        new_elem = copy.deepcopy(elem)
        s = ET.tostring(new_elem, encoding='unicode')
        # indent plural items
        additions.append('    ' + s)
    
    insert = '\n'.join(additions)
    # Insert before </resources>
    new_content = content.rstrip()
    if new_content.endswith('</resources>'):
        new_content = new_content[:-len('</resources>')] + insert + '\n</resources>\n'
    else:
        new_content = new_content + '\n' + insert + '\n</resources>\n'
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)


def main():
    en_tree, en_root = parse_ordered(EN_FILE)
    en_strings, en_plurals, en_order = get_en_entries(en_root)
    
    print(f'EN: {len(en_strings)} strings, {len(en_plurals)} plurals')
    
    locale_files = sorted(glob.glob(os.path.join(RES_DIR, 'values-*', 'strings.xml')))
    
    for lf in locale_files:
        locale = lf.split('values-')[1].split(os.sep)[0]
        count = fill_locale(lf, en_strings, en_plurals)
        if count > 0:
            print(f'{locale}: added {count} missing entries')
        else:
            print(f'{locale}: up to date')


if __name__ == '__main__':
    main()
