#!/usr/bin/env python3
"""Make an Opentheso SKOS export importable by Opentheso again.

Two things in a real export abort an import:

1. An rdf:about that is not an IRI. Opentheso stores whatever an editor typed into a URI
   field, and RDF4J is strict:
       RDFParseException: Unexpected character U+5B at index 0: [[File:...|...]]
   The whole rdf:Description is dropped.

2. Two dcterms with the same name and value on the ConceptScheme, differing only by
   xml:lang. thesaurus_dcterms is UNIQUE (id_thesaurus, name, value) -- language is not
   in the key -- so the second one aborts the transaction:
       ERROR: duplicate key value violates unique constraint "thesaurus_dcterms_uniquekey"
   First occurrence wins; later duplicates are dropped. Per-language names survive in
   skos:prefLabel regardless.

   Changeset thesaurus-dcterms-language-key fixes the key, so (2) is redundant against a
   patched instance -- it is kept for exports headed anywhere else.

Usage:  python3 data/clean-rdf.py in.rdf out.rdf
"""
import html
import re
import sys

# ponytail: character blacklist, not a full RFC 3987 check -- these are the chars RDF4J
# rejects outright, and they are what a human paste accident actually contains.
ILLEGAL = set(' <>"{}|\\^`') | {chr(c) for c in range(0x21)}

ABOUT = re.compile(r'<rdf:Description\s[^>]*rdf:about="([^"]*)"')
DCTERM = re.compile(r'<dcterms:(\w+)[^>]*>(.*?)</dcterms:\1>')
END = '</rdf:Description>'


def bad_iri(iri):
    return any(ch in ILLEGAL for ch in html.unescape(iri))


def main(src, dst):
    dropped, deduped = [], []
    skipping = False
    in_scheme = False       # inside the ConceptScheme's Description
    seen = set()            # (name, value) already emitted for the scheme

    with open(src, encoding='utf-8') as fin, open(dst, 'w', encoding='utf-8') as fout:
        for line in fin:
            if skipping:
                # the export writes one Description per block, closing tag on its own line
                if END in line:
                    skipping = False
                continue

            m = ABOUT.search(line)
            if m:
                if bad_iri(m.group(1)):
                    dropped.append(m.group(1))
                    skipping = END not in line
                    continue
                in_scheme = False   # a new block; the type element decides below

            if 'ConceptScheme' in line:
                in_scheme = True
            elif END in line:
                in_scheme = False
            elif in_scheme:
                d = DCTERM.search(line)
                if d:
                    key = (d.group(1), html.unescape(d.group(2)))
                    if key in seen:
                        deduped.append(key)
                        continue
                    seen.add(key)

            fout.write(line)

    print(f'dropped {len(dropped)} description(s) with an unusable rdf:about')
    for iri in dropped:
        print(f'  {iri[:120]}')
    print(f'dropped {len(deduped)} duplicate dcterms on the ConceptScheme')
    for name, value in deduped:
        print(f'  dcterms:{name} = {value[:100]}')
    return 0


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1], sys.argv[2]))
