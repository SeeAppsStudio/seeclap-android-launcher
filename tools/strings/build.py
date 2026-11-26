#!/usr/bin/env python3
"""Génère les textes de l'accueil dans les 12 langues de SeeClap.

Chaque langue est un fichier <code>.py qui définit T = {clé: texte}. Le français (fr.py) fait foi :
une clé qui manque dans une autre langue, ou une clé en trop, arrête la génération.
Les %d, %s, %1$s… doivent être les mêmes dans toutes les langues.

    python3 tools/strings/build.py
"""
import importlib.util
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "app", "src", "main", "res")
# code de fichier -> dossier Android (l'anglais est la langue par défaut)
LANGS = {"en": "values", "fr": "values-fr", "de": "values-de", "es": "values-es", "it": "values-it",
         "nl": "values-nl", "pt": "values-pt", "ru": "values-ru", "sv": "values-sv", "tr": "values-tr",
         "zh": "values-zh", "hi": "values-hi"}
FORMAT = re.compile(r"%(?:\d+\$)?[ds%]")


def load(code):
    spec = importlib.util.spec_from_file_location(code, os.path.join(HERE, code + ".py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod.T


def escape(text):
    text = text.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if text[:1] in ("@", "?"):
        text = "\\" + text
    return text


def main():
    ref = load("fr")
    errors = []
    tables = {}
    for code in LANGS:
        t = load(code)
        missing = [k for k in ref if k not in t]
        extra = [k for k in t if k not in ref]
        for k in ref:
            if k in t and not t[k].strip():
                errors.append(f"{code}: texte vide pour « {k} »")
            if k in t and sorted(FORMAT.findall(ref[k])) != sorted(FORMAT.findall(t[k])):
                errors.append(f"{code}: formats différents pour « {k} »")
        if missing:
            errors.append(f"{code}: il manque {', '.join(missing)}")
        if extra:
            errors.append(f"{code}: en trop {', '.join(extra)}")
        tables[code] = t
    if errors:
        print("\n".join(errors))
        sys.exit(1)
    for code, folder in LANGS.items():
        out = os.path.join(RES, folder)
        os.makedirs(out, exist_ok=True)
        lines = ['<?xml version="1.0" encoding="utf-8"?>',
                 "<!-- Généré par tools/strings/build.py : modifier tools/strings/%s.py, pas ce fichier. -->" % code,
                 "<resources>"]
        for k in ref:
            if k == "app_name" and folder != "values":
                continue
            v = tables[code][k]
            attr = ' translatable="false"' if k == "app_name" else ""
            fmt = ' formatted="false"' if "%" in v and not FORMAT.search(v) else ""
            lines.append(f'    <string name="{k}"{attr}{fmt}>{escape(v)}</string>')
        lines.append("</resources>")
        with open(os.path.join(out, "strings.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
    print(f"{len(ref)} textes × {len(LANGS)} langues")


if __name__ == "__main__":
    main()
