#!/usr/bin/env python3
'''
This scripts mirrors the Bible from AELF.org and post-processes it so that it
becomes suitable for the application.
Mirrored intermediate files are cached for best performance but not versionned
in GIT. Final output is mirrored in GIT.
'''

import os
import sys
import glob
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

#
# Main
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

# Post-process the Bible
for chapter_file_path in glob.glob('%s/*/*.html' % BIBLE_CACHE_FOLDER):
    print("\u001b[KINFO: Processing %s..." % (chapter_file_path), end='\r')

    # Extract book reference and chapter reference from path
    book_ref, chapter_ref = chapter_file_path.rsplit('.', 1)[0].rsplit('/', 2)[1:]

    # Rewrite the XXX book ref of Jeremy's letter to 1Jr. This is not strictly
    # right but it is less suggestive...
    if book_ref == 'XXX':
        book_ref = '1Jr'

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

    # Save the chapter
    dest_folder = os.path.join(BIBLE_DEST_FOLDER, book_ref)
    dest_file = os.path.join(dest_folder, '%s.html' % (chapter_ref))

    os.makedirs(dest_folder, exist_ok=True)
    with open(dest_file, 'w') as f:
        f.write(str(final_elem))

print('\u001b[K', end='\r')
print("INFO: All done!")

