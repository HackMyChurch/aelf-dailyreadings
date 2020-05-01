#!/usr/bin/env python3
'''
This scripts mirrors the Bible from AELF.org and post-processes it so that it
becomes suitable for the application.

The post-processing includes:
- Keep only the bible markup, remove all AELF website specific markup
- Index the pages so that they can be used for the application search engine

Mirrored intermediate files are cached for best performance but not versionned
in GIT. Final output is mirrored in GIT.
'''

import os
import re
import sys
import glob
import sqlite3
import subprocess

from bs4 import BeautifulSoup

#
# Configuration
#

CACHE_FOLDER = "./tmp"
BIBLE_DOMAIN = "www.aelf.org"
BIBLE_PATH = "bible"
ASSETS_FOLDER = "./app/src/main/assets"

BIBLE_CACHE_FOLDER = os.path.join(CACHE_FOLDER, BIBLE_DOMAIN, BIBLE_PATH)
BIBLE_DEST_FOLDER = os.path.join(ASSETS_FOLDER, BIBLE_PATH)

BIBLE_DB = os.path.join(ASSETS_FOLDER, 'bible.db')

BIBLE_BOOKS = {
    'Ancien Testament': {
        'Pentateuque': {
            'Gn': {'title': 'La Genèse'},
            'Ex': {'title': 'L\'Exode'},
            'Lv': {'title': 'Le Lévitique'},
            'Nb': {'title': 'Les Nombres'},
            'Dt': {'title': 'Le Deutéronome'},
        },
        'Livres Historiques': {
            'Jos': {'title': 'Le Livre de Josué'},
            'Jg':  {'title': 'Le Livre des Juges'},
            'Rt':  {'title': 'Le Livre de Ruth'},
            '1S':  {'title': 'Premier Livre de Samuel'},
            '2S':  {'title': 'Deuxième Livre de Samuel'},
            '1R':  {'title': 'Premier Livre des Rois'},
            '2R':  {'title': 'Deuxième Livre des Rois'},
            '1Ch': {'title': 'Premier Livre des Chroniques'},
            '2Ch': {'title': 'Deuxième Livre des Chroniques'},
            'Esd': {'title': 'Le Livre d\'Esdras'},
            'Ne':  {'title': 'Le Livre de Néhémie'},
            'Tb':  {'title': 'Tobie'},
            'Jdt': {'title': 'Judith'},
            'Est': {'title': 'Esther'},
            '1M':  {'title': 'Premier Livre des Martyrs d\'Israël'},
            '2M':  {'title': 'Deuxième Livre des Martyrs d\'Israël'},
        },
        'Livres Poètiques et Sapientiaux': {
            'Jb': {'title': 'Job'},
            'Pr': {'title': 'Les Proverbes'},
            'Qo': {'title': 'L\'Écclésiaste (Qohélet)'},
            'Ct': {'title': 'Le Cantique des Cantiques'},
            'Sg': {'title': 'Le Livre de la Sagesse'},
            'Si': {'title': 'L\'Écclésiastique (Siracide)'},
        },
        'Livres Prophètiques': {
            'Is':  {'title': 'Isaïe'},
            'Jr':  {'title': 'Jérémie'},
            'Lm':  {'title': 'Les Lamentations'},
            'Ba':  {'title': 'Baruch'},
            '1Jr': {'title': 'Lettre de Jérémie', 'path': 'XXX'},
            'Ez':  {'title': 'Ézéchiel'},
            'Dn':  {'title': 'Daniel'},
            'Os':  {'title': 'Osée'},
            'Jl':  {'title': 'Joël'},
            'Am':  {'title': 'Amos'},
            'Ab':  {'title': 'Abdias'},
            'Jon': {'title': 'Jonas'},
            'Mi':  {'title': 'Michée'},
            'Na':  {'title': 'Nahum'},
            'Ha':  {'title': 'Habaquq'},
            'So':  {'title': 'Sophonie'},
            'Ag':  {'title': 'Aggée'},
            'Za':  {'title': 'Zacharie'},
            'Ml':  {'title': 'Malachie'},
        },
    },
    'Nouveau Testament': {
        'Évangiles': {
            'Mt': {'title': 'Évangile selon Saint Matthieu'},
            'Mc': {'title': 'Évangile selon Saint Marc'},
            'Lc': {'title': 'Évangile selon Saint Luc'},
            'Jn': {'title': 'Évangile selon Saint Jean'},
        },
        'Actes': {
            'Ac': {'title': 'Les Actes des Apôtres'},
        },
        'Épitres de Saint Paul': {
            'Rm':  {'title': 'Aux Romains'},
            '1Co': {'title': 'Première aux Corinthiens'},
            '2Co': {'title': 'Deuxième aux Corinthiens'},
            'Ga':  {'title': 'Aux Galates'},
            'Ep':  {'title': 'Aux Éphésiens'},
            'Ph':  {'title': 'Aux Philippiens'},
            'Col': {'title': 'Aux Colossiens'},
            '1Th': {'title': 'Première aux Théssaloniciens'},
            '2Th': {'title': 'Deuxième aux Théssaloniciens'},
            '1Tm': {'title': 'Première à Timothée'},
            '2Tm': {'title': 'Deuxième à Timothée'},
            'Tt':  {'title': 'À Tite'},
            'Phm': {'title': 'À Philémon'},
        },
        'Épîtres Catholiques': {
            'He':   {'title': 'Épître aux Hébreux'},
            'Jc':   {'title': 'Épître de Saint Jacques'},
            '1P':   {'title': 'Premier Épître de Saint Pierre'},
            '2P':   {'title': 'Deuxième Épître de Saint Pierre'},
            '1Jn':  {'title': 'Premier Épître de Saint Jean'},
            '2Jn':  {'title': 'Deuxième Épître de Saint Jean'},
            '3Jn':  {'title': 'Troisième Épître de Saint Jean'},
            'Jude': {'title': 'Épître de Saint Jude'},
        },
        'Apocalypse': {
            'Ap': {'title': 'L\'Apocalypse'},
        },
    },
}

BIBLE_PSALMS = {}
for i in range(1, 151):
    if i in [9, 113]:
        BIBLE_PSALMS[f'Ps{i}A'] = {'title': f'Psaume {i}A'}
        BIBLE_PSALMS[f'Ps{i}B'] = {'title': f'Psaume {i}B'}
    else:
        BIBLE_PSALMS[f'Ps{i}'] = {'title': f'Psaume {i}'}

#
# Mirror
#

# Move to the project root
os.chdir(os.path.join(sys.path[0], '..'))

# Mirror the Bible to a .gitignored folder
if os.path.isdir(BIBLE_CACHE_FOLDER):
    print("INFO: The Bible has already been downloaded. Skipping.")
    print("TIP: to force Bible downloading, remove %s" % (BIBLE_CACHE_FOLDER))
else:
    print("INFO: Mirroring the Bible locally")
    os.makedirs(CACHE_FOLDER, exist_ok=True)
    subprocess.run(
            [
                'wget',
                'https://%s/%s' % (BIBLE_DOMAIN , BIBLE_PATH),
                '--directory-prefix', CACHE_FOLDER,
                '--accept-regex', 'bible/.*',
                '--tries', '3',
                '--recursive', '--no-clobber', '--continue', '--no-parent', '--force-directories', '--adjust-extension',
            ],
            check=True,
    )


#
# Prepare the index
#

# Destroy existing index
if os.path.isfile(BIBLE_DB):
    os.unlink(BIBLE_DB)

conn = sqlite3.connect(BIBLE_DB)
cursor = conn.cursor()

# Create Bible table. Typical access pattern:
# - get ordered list of chapters of a book
# - get full chapter of book, ordered by verses
cursor.execute('''
CREATE TABLE verses (
    book          TEXT,
    book_id       INTEGER,
    book_title    TEXT,
    chapter       TEXT,
    chapter_id    INTEGER,
    chapter_title TEXT,
    verse         INTEGER,
    text          TEXT,
    PRIMARY KEY (book, chapter, verse)
);''')

# Create the chapter view. Aggregates the verses of a chapter.
# This glue layer between the verses and the index to share the same data
cursor.execute('''
CREATE VIEW chapters (
    book,
    book_id,
    chapter,
    chapter_id,
    title,
    text
) AS
    SELECT book, book_id, chapter, chapter_id, book_title || ', ' || chapter_title, group_concat(text, '\n')
    FROM verses
    GROUP BY chapter_id
    ORDER BY book, chapter, verse
;''')

# Create the index
cursor.execute('''
CREATE VIRTUAL TABLE search USING fts5(
    book     UNINDEXED,
    book_id  UNINDEXED,
    chapter  UNINDEXED,
    title    UNINDEXED,
    text,
    content=chapters,
    content_rowid=chapter_id,
    tokenize = 'unicode61 remove_diacritics 2',
);''')

#
# Global state
#

book_id = 0
chapter_id = 0

#
# Process
#

def index_path(chapter_file_path, book_ref, book_id, book_title, chapter_ref, chapter_title):
    global chapter_id

    print("\u001b[KINFO: Processing %s..." % (chapter_file_path), end='\r')

    # Parse the HTML
    with open(chapter_file_path) as f:
        soup = BeautifulSoup(f.read(), 'html.parser')

    # Find the actual text and rewrite the markup
    chapter_soup = soup.find(class_='block-single-reading')
    final_elem = soup.new_tag('p')
    for chapter_verse in chapter_soup.find_all('p', recursive=False):
        # Get the verse identifier and set properties (.text-danger is used in the psalms)
        verse_ref_elem = chapter_verse.select_one(".verse_number, .text-danger")
        verse_ref_elem['aria-hidden'] = "true"
        verse_ref_elem['class'] = 'verse'
        verse_ref = verse_ref_elem.text.lower().lstrip('0')
        verse_ref_elem.string = verse_ref

        # Set verse properties
        chapter_verse.name = 'span'
        chapter_verse['class'] = 'line'
        chapter_verse['id'] = "verse-%s" % (verse_ref)
        chapter_verse['tabindex'] = '0'

        # Register the rewritten verse
        final_elem.append(chapter_verse)

        # Insert the verse in the database
        verse_text = chapter_verse.span.next_sibling.strip()
        try:
            if not verse_ref:
                verse_ref = None
            cursor.execute(
                    '''INSERT INTO verses(book, book_id, book_title, chapter, chapter_id, chapter_title, verse, text) VALUES(?, ?, ?, ?, ?, ?, ?, ?);''',
                    (book_ref, book_id, book_title, chapter_ref, chapter_id, chapter_title, verse_ref, verse_text)
            )
        except:
            print('\nERROR: Failed to insert verse:', book_ref, chapter_ref, verse_ref, chapter_verse, verse_text)
    chapter_id += 1

    # Save the chapter
    dest_folder = os.path.join(BIBLE_DEST_FOLDER, book_ref)
    dest_file = os.path.join(dest_folder, '%s.html' % (chapter_ref))

    os.makedirs(dest_folder, exist_ok=True)
    with open(dest_file, 'w') as f:
        f.write(str(final_elem))

# Post-process the Bible books
for part_title, part in BIBLE_BOOKS.items():
    for section_title, section in part.items():
        for book_ref, book in section.items():
            book_id += 1
            book_path = book.get('path', book_ref)
            book_title = book['title']
            for chapter_file_path in glob.glob(f'{BIBLE_CACHE_FOLDER}/{book_path}/*.html'):
                chapter_ref = chapter_file_path.rsplit('/')[-1].split('.')[0]
                if chapter_ref == "0" and book_ref == "Est":
                    chapter_title = f'Préliminaires'
                elif chapter_ref == "0" and book_ref == "Si":
                    chapter_title = f'Prologue'
                else:
                    chapter_title = f'Chapitre {chapter_ref}'
                index_path(chapter_file_path, book_ref, book_id, book_title, chapter_ref, chapter_title)

# Post-process the Bible psalms
for psalm_ref, psalm in BIBLE_PSALMS.items():
    book_id += 1
    book_ref = 'Ps'
    chapter_ref = psalm_ref[2:]
    chapter_file_path = f'{BIBLE_CACHE_FOLDER}/{book_ref}/{chapter_ref}.html'
    chapter_title = psalm['title']
    index_path(chapter_file_path, book_ref, book_id, "Livre des Psaumes", chapter_ref, chapter_title)

# Index all chapters
cursor.execute('''INSERT INTO search(book, book_id, chapter, rowid, title, text) SELECT book, book_id, chapter, chapter_id, title, text FROM chapters;''')

# Optimize the index
print("\u001b[KINFO: Optimizing the index...", end='\r')
cursor.execute('''INSERT INTO search(search) VALUES('optimize');''')

# Apply all changes
print("\u001b[KINFO: Saving the index...", end='\r')
conn.commit()

# Optimize the database
print("\u001b[KINFO: Optimizing the database...", end='\r')
cursor.execute('''VACUUM;''')

print('\u001b[K', end='\r')
print("INFO: All done!")

