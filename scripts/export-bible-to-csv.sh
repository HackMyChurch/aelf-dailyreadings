#!/bin/bash
# This trivial script exports the content of the DB as CSV to make it easy to compare revisions of the Bible.

set -eiu

sqlite3 app/src/main/assets/bible.db -csv 'select book, chapter, verse, text from verses order by book, CAST(chapter as INTEGER), chapter, CAST(verse as INTEGER)'

