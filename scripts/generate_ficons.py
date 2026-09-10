#!/usr/bin/env python3
"""Regenerate ficons file-type SVGs in Material You (MD3) flat tonal style.

Writes the full set into both consumer directories:
  app/src/main/assets/ficons/       (Android, loaded via Coil SVG decoder)
  app/src/main/resources/web/ficons/ (plain-desktop web UI)

Usage: python3 scripts/generate_ficons.py [--review DIR]
  --review DIR also writes review.html + a copy of the set into DIR for visual review.
"""

import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGETS = [
    os.path.join(ROOT, "app/src/main/assets/ficons"),
    os.path.join(ROOT, "app/src/main/resources/web/ficons"),
]

# (container, fold, on-text) — MD3 tonal pastels
PALETTES = {
    "red":    ("#FFDAD6", "#FFB4A8", "#8C1D18"),  # pdf
    "amber":  ("#FFDDB0", "#FFC46B", "#5C4300"),  # presentation
    "green":  ("#C9EFC5", "#9CD67E", "#1E4D18"),  # spreadsheet / office data
    "teal":   ("#C2EBDD", "#8CD4BC", "#0B4F3A"),  # image / cad / 3d
    "blue":   ("#D3E4FF", "#A5CBFF", "#0D2C55"),  # document / text
    "indigo": ("#E1E2FF", "#BFC2FF", "#252766"),  # code / config
    "purple": ("#EBDCFF", "#D1BCFF", "#48218A"),  # audio
    "pink":   ("#FFD9E7", "#FFB1CC", "#6E1147"),  # video
    "brown":  ("#EFDCBE", "#D9BC90", "#4B3415"),  # archive
    "lime":   ("#E9F0B0", "#D2E277", "#3E4E10"),  # font
    "slate":  ("#DFE3EA", "#C3C9D6", "#22293A"),  # exec / disk / security / misc
}

CATEGORY = {}


def assign(color, exts):
    for e in exts.split():
        CATEGORY[e] = color


assign("red", "pdf")
assign("blue", """
doc docb docm docx dot dotm dotx ott odt rtf txt log nfo info diz me mi wbk wps pages
msg eml fax hlp chm xps ics cal vcs vcf epub ibooks kf8 fb2 lit mobi md rst tex ost pst
""")
assign("green", "csv tsv xls xlsm xlsx xlt xltm xltx xlm ods accdb accdt adn mdb dbf rpt tax odb")
assign("amber", "ppt pptm pptx pps ppsx pot potx sldm sldx odp key mpp mpt cptx pub")
assign("purple", """
aa aac aif aifc aiff amr au aup caf cdda cue flac m3u m3u8 m4a m4r
mid midi mp3 mpga ogg ra ram vox wav wma 3ga mpd xspf
""")
assign("pink", """
3g2 3gp asf asx avi f4v flv m2v m4v mkv mod mov mp2 mp4 mpa mpe mpeg mpg
ogv qt rm vob webm wmv wmx ifo fla xfl swf
""")
assign("teal", """
ai bmp bpg cr2 cur ani dgn dng dwg dxf eps gif heic icns ico idf iff image
j2 jpe jpeg jpg mng nef pcd png ps psd psp raw svg tga tif tiff webp wmf xcf
indd step stl cad
""")
assign("indigo", """
as asax ascx ash ashx asm asmx asp aspx axd applescript c cpp cs csh cson css coffee
coffeelintignore compile cfg cfm cfml cgi conf config csproj dart dsn dtd dpj el gem gpl gradle go h
handlebars hbs hs hsl htm html in inc ini java js json jsp jsx kmk kup kt kts
less lex lisp lock lua m m4 master mc map mk mm mo nix rdf ph phar php pl plist pm po
pom prop ps1 py pyc rb resx rdl rss rub sass scss sed sh skin sln
sitemap sphinx sql swift tfignore tmx tpl ts twig vb vbproj vbs vdx vsd vss vst
vsx vtx webinfo wsf xaml xml xsd xsl xrb yaml yml zsh bash ksh bat cmd tcsh
bowerrc editorconfig eslintignore gitattributes gitignore npmignore vscodeignore
code-workspace codekit iml sln reg inf
""")
assign("brown", "7z ace aze bz2 bzempty cab gz rar sit tar tgz xz z zip jar war xpi bak")
assign("lime", "eot fnt fon otf ttf woff woff2")
assign("slate", """
apk app bin class com dll elf exe gadget msi msu deb rpm pkg dmg iso img udf
vcd ova ovf vdi vmdk nes rom torrent dat db mdf sdf pdb sqlite idx lnk
ds_store dist tmp part crdownload download pid swp swd sys ocx retry ru sol gdp
cer crt pem pfx p7b p12 gpg pgp asc enc crypt rsa licx inv ac ait browser cd data
""")

# drop junk keys defensively
CATEGORY = {k: v for k, v in CATEGORY.items() if not k.endswith("?")}

FOLD = 26


def page_path(r=8):
    return (f"M{r} 0 H{72 - FOLD} L72 {FOLD} V{100 - r} "
            f"Q72 100 {72 - r} 100 H{r} Q0 100 0 {100 - r} V{r} Q0 0 {r} 0 Z")


def fold_path():
    return f"M{72 - FOLD} 0 V{FOLD} H72 Z"


def label_font_size(text):
    n = len(text)
    return min(20, round(54 / (0.62 * n), 1))


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def gen_svg(ext, palette_key, with_label=True):
    container, fold, on = PALETTES[palette_key]
    label_part = ""
    if with_label:
        label = ext.upper()
        fs = label_font_size(label)
        label_part = (f'<text x="36" y="66" font-family="system-ui,\'Segoe UI\',Roboto,sans-serif" '
                      f'font-size="{fs}" font-weight="600" fill="{on}" '
                      f'text-anchor="middle">{esc(label)}</text>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 100">\n'
            f'<path d="{page_path()}" fill="{container}"/>\n'
            f'<path d="{fold_path()}" fill="{fold}"/>\n'
            f'{label_part}</svg>\n')


def gen_folder():
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 55">\n'
            '<path d="M8 5 H26 L32 11 H64 Q68 11 68 15 V21 H4 V9 Q4 5 8 5 Z" fill="#E5AE54"/>\n'
            '<path d="M8 14 H64 Q68 14 68 18 V46 Q68 50 64 50 H8 Q4 50 4 46 V18 Q4 14 8 14 Z" fill="#FFD790"/>\n'
            '</svg>\n')


def build_set(src_dir):
    """Returns {ext: svg_text} for every .svg in src_dir."""
    exts = sorted(f[:-4] for f in os.listdir(src_dir) if f.endswith(".svg"))
    special = {"folder", "default", "blank"}
    uncovered = [e for e in exts if e not in CATEGORY and e not in special]
    for e in uncovered:
        CATEGORY[e] = "slate"
    if uncovered:
        print("fallback(slate):", " ".join(uncovered))
    out = {}
    for e in exts:
        if e == "folder":
            out[e] = gen_folder()
        elif e in ("default", "blank"):
            out[e] = gen_svg(e, "slate", with_label=False)
        else:
            out[e] = gen_svg(e, CATEGORY[e])
    return out


def write_review(review_dir, exts):
    groups = {}
    for e in exts:
        key = "special" if e in ("folder", "default", "blank") else CATEGORY.get(e, "slate")
        groups.setdefault(key, []).append(e)
    order = ["red", "blue", "green", "amber", "purple", "pink", "teal", "indigo",
             "brown", "lime", "slate", "special"]
    names = {"red": "PDF", "blue": "Document", "green": "Spreadsheet", "amber": "Presentation",
             "purple": "Audio", "pink": "Video", "teal": "Image/CAD/3D", "indigo": "Code/Config",
             "brown": "Archive", "lime": "Font", "slate": "Exec/Disk/Misc", "special": "Special"}
    os.makedirs(os.path.join(review_dir, "svg"), exist_ok=True)
    cells = []
    for key in order:
        if key not in groups:
            continue
        cells.append(f'<h2>{names[key]} <span class="cnt">{len(groups[key])}</span></h2><div class="grid">')
        for e in groups[key]:
            with open(os.path.join(review_dir, "svg", f"{e}.svg"), "w") as f:
                f.write(SET[e])
            cells.append(f'<div class="cell"><img src="svg/{e}.svg"><span>{e}</span></div>')
        cells.append("</div>")
    html = f'''<!doctype html><html><head><meta charset="utf-8"><style>
body {{ background:#fff; color:#111; font-family:-apple-system,sans-serif; margin:24px; }}
h2 {{ font-size:15px; font-weight:600; margin:22px 0 8px; }}
.cnt {{ color:#888; font-weight:400; }}
.grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(84px,1fr)); gap:14px; max-width:960px; }}
.cell {{ text-align:center; font-size:11px; color:#444; }}
.cell img {{ height:72px; }}
</style></head><body>
<h1 style="font-size:18px">ficons — Material You ({len(exts)} icons)</h1>
{''.join(cells)}
</body></html>'''
    with open(os.path.join(review_dir, "review.html"), "w") as f:
        f.write(html)


if __name__ == "__main__":
    import sys
    args = sys.argv[1:]
    targets = []
    review_dir = None
    i = 0
    while i < len(args):
        if args[i] == "--review":
            review_dir = args[i + 1]
            i += 2
        else:
            targets.append(args[i])
            i += 1
    exts = None
    for target in (targets or TARGETS):
        SET = build_set(target)
        for name, svg in SET.items():
            with open(os.path.join(target, f"{name}.svg"), "w") as f:
                f.write(svg)
        exts = sorted(SET)
        print(f"{target}: {len(exts)} icons")
    if review_dir:
        write_review(review_dir, exts)
