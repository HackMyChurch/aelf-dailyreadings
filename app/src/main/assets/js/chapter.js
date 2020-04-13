// Highlight search keywords
if (highlight) {
    var instance = new Mark(document.querySelector("body"));
    instance.mark(highlight, {
        "accuracy": {
            "value": "exactly",
            "limiters": " : “’”:;.,-–—‒_(){}[]!'\"+=".split(""),
        },
        "ignoreJoiners": true,
        "acrossElements": true,
        "wildcards": "enabled",
        "ignorePunctuation": "’-–—‒_".split(""),
    });
}

// Highlight reference
/*
 * Return -1 if chapter_1 <  chapter_2
 * Return  0 if chapter_1 == chapter_2
 * Return  1 if chapter_1 >  chapter_2
 */
function compare_chapters(chapter_1, chapter_2) {
    match_1 = chapter_1.match(/^([0-9]+)([a-z]?)$/i);
    match_2 = chapter_2.match(/^([0-9]+)([a-z]?)$/i);

    number_1 = parseInt(match_1[0]);
    number_2 = parseInt(match_2[0]);
    letter_1 = match_1[1];
    letter_2 = match_2[1];

    if (number_1 < number_2) {
        return -1;
    }

    if (number_1 > number_2) {
        return 1;
    }

    if (letter_1 < letter_2) {
        return -1;
    }

    if (letter_1 > letter_2) {
        return 1;
    }

    return 0;
}

/*
 * Parse a Bible reference into a list of start chapter/verse to end
 * chapter/verse.
 */
function parse_reference(reference) {
    // Remove letters after the verses
    reference = reference.replace(/[a-z]*/g, "")

    // Start the parsing
    var ranges = [];
    var state = 'chapter_start';
    var current = {};
    while (reference) {
        // Parse a chunk
        match = reference.match(/^([0-9]+[A-Z]*)(.?)(.*)$/);
        number = match[1];
        separator = match[2];
        reference = match[3];

        switch (state) {
            // State: chapter_start
            case 'chapter_start':
                current = {
                    'chapter_start': number,
                    'verse_start': 0,
                    'chapter_end': number,
                    'verse_end': Infinity
                };
                switch (separator) {
                    case "":
                        ranges.push(current);
                        current = null;
                        reference = null;
                        break;
                    case ",":
                        state = 'verse_start';
                        break;
                    case "-":
                        state = 'chapter_end';
                        break;
                    default:
                        console.error("Failed to parse reference: invalid separator '" + separator + "'");
                        reference = null;
                        break;
                }
                break;

                // State: verse_start
            case 'verse_start':
                current['verse_start'] = parseInt(number);
                current['verse_end'] = current['verse_start'];
                switch (separator) {
                    case "":
                        ranges.push(current);
                        current = {
                            'chapter_start': current['chapter_end'],
                            'verse_start': 0,
                            'chapter_end': current['chapter_end'],
                            'verse_end': 0
                        };
                        reference = null;
                        break;
                    case ".":
                    case ",":
                        current['verse_end'] = current['verse_start']
                        ranges.push(current);
                        current = {
                            'chapter_start': current['chapter_end'],
                            'verse_start': 0,
                            'chapter_end': current['chapter_end'],
                            'verse_end': 0
                        };
                        state = 'verse_start';
                        break;
                   case ";":
                       ranges.push(current);
                       current = null;
                       state = 'chapter_start';
                       break;
                    case "-":
                        state = 'verse_end';
                        break;
                    case "–":
                        current['verse_end'] = Infinity
                        state = 'chapter_end';
                        break;
                    default:
                        console.error("Failed to parse reference: invalid separator '" + separator + "'");
                        reference = null;
                        break;
                }
                break;

                // State: verse_end
            case 'verse_end':
                current['verse_end'] = parseInt(number);
                switch (separator) {
                    case "":
                        ranges.push(current);
                        current = null;
                        reference = null;
                        break;
                    case ".":
                    case ",":
                        ranges.push(current);
                        current = {
                            'chapter_start': current['chapter_end'],
                            'verse_start': 0,
                            'chapter_end': current['chapter_end'],
                            'verse_end': 0
                        };
                        state = 'verse_start';
                        break;
                    case ";":
                        ranges.push(current);
                        current = null;
                        state = 'chapter_start';
                        break;
                    default:
                        console.error("Failed to parse reference: invalid separator '" + separator + "'");
                        reference = null;
                        break;
                }
                break;

                // State: chapter_end
            case 'chapter_end':
                current['chapter_end'] = number;
                current['verse_end'] = Infinity;
                switch (separator) {
                    case "":
                        ranges.push(current);
                        current = null;
                        reference = null;
                        break;
                    case ",":
                        state = 'verse_end';
                        break;
                    case ".":
                        ranges.push(current);
                        current = {
                            'chapter_start': current['chapter_end'],
                            'verse_start': 0,
                            'chapter_end': current['chapter_end'],
                            'verse_end': 0
                        };
                        state = 'verse_start';
                        break;
                    case ";":
                        ranges.push(current);
                        current = null;
                        state = 'chapter_start';
                        break;
                    default:
                        console.error("Failed to parse reference: invalid separator '" + separator + "'");
                        reference = null;
                        break;
                }
                break;

                // Invalid state
            default:
                console.error("Failed to parse reference: invalid state '" + state + "'");
                reference = null;
                break;
        }
    }

    // All done
    return ranges;
}

/*
 * Add class 'highlight' to verses in the range
 */
function highlight_range(verse_start, verse_end) {
    verses = document.querySelectorAll('[id ^= verse-]');
    for (var i = 0; i < verses.length; i++) {
        verse = verses[i];
        verse_id = parseInt(verse.id.split('-')[1]);
        if (verse_id >= verse_start && verse_id <= verse_end) {
            verse.classList.add('highlight');
        }
    }
}

/*
 * Parse a reference and highlight matching verses in the selected chapter.
 */
function highlight_reference_for_chapter(reference, chapter) {
    var reference_ranges = parse_reference(reference);
    var chapter_ranges = [];
    var first_verse_id = Infinity;

    for (var i = 0; i < reference_ranges.length; i++) {
        var reference_range = reference_ranges[i];

        compare_start = compare_chapters(chapter, reference_range['chapter_start']);
        compare_end = compare_chapters(chapter, reference_range['chapter_end']);

        if (compare_start < 0 || compare_end > 0) {
            // Before or after the range
            continue;
        }

        if (compare_start == 0 && compare_end == 0) {
            // Full range is in chapter
            highlight_range(reference_range['verse_start'], reference_range['verse_end']);
            if (first_verse_id > reference_range['verse_start']) {
                first_verse_id = reference_range['verse_start'];
            }
            continue;
        }

        if (compare_start > 0 && compare_end < 0) {
            // Chapter is fully contained in range
            highlight_range(0, Infinity);
            first_verse_id = 0;
            continue;
        }

        if (compare_start == 0) {
            // Chapter contains the first part of the range
            highlight_range(reference_range['verse_start'], Infinity);
            if (first_verse_id > reference_range['verse_start']) {
                first_verse_id = reference_range['verse_start'];
            }
            continue;
        }

        if (compare_end == 0) {
            // Chapter contains the end part of the range
            highlight_range(0, reference_range['verse_end']);
            first_verse_id = 0;
            continue;
        }
    }

    // Move the first highlighted element in the visible area
    first_verse_element = document.getElementById('verse-'+first_verse_id);
    if (first_verse_element) {
        first_verse_element.scrollIntoView();
    }
}

if (reference) {
    highlight_reference_for_chapter(reference, current_chapter);
}
