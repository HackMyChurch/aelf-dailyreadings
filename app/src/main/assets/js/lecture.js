/*
 * Helpers
 */

String.prototype.capitalize = function() {
  return this.charAt(0).toUpperCase() + this.slice(1)
}

/*
 * Detect references in the lecture and linkify them.
 */
var references_elements = document.querySelectorAll('small i');
for (var i = 0; i < references_elements.length; i++) {
    // Extract reference
    var references_element = references_elements[i];
    var reference_text = references_element.textContent.slice(1);
    var reference = reference_text;

    // Extract "Cantiques" references
    if (reference.match(/^CANTIQUE/)) {
        reference = reference.split('(', 1)[1].rsplit(')', 1)[0]
    }

    // Clean extracted reference
    reference = reference.toLowerCase();
    reference = reference.replace(/\s*/gi, "");
    reference = reference.replace(/\([0-9]*[A-Z]?\)/gi, "");

    // Do we still have something to parse ?
    if (!reference) {
        continue;
    }

    // Extract the main reference chunks
    var matches = reference.match(/([0-9]?)([a-z]+)([0-9]*[a-b]*)(,?)(.*?)(?:-[iv]+)*$/);
    if (!matches) {
        continue;
    }

    var book_number = matches[1];
    var book_name   = matches[2];
    var chapter     = matches[3];
    var comma       = matches[4];
    var rest        = matches[5];

    // Build the link
    var verses = chapter.toUpperCase()+comma+rest;
    var link = "https://www.aelf.org/bible/"+book_number+book_name.capitalize()+"/"+chapter+"?reference="+verses;

    // Inject the link
    references_element.innerHTML = '— <a href="'+link+'">'+reference_text+'</a>';
}
