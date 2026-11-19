/**
 * AELF Bible Bookmarks & Highlights module
 */
(function() {
    'use strict';

    /**
     * Returns clean text nodes inside an element, ignoring verse numbers (<span class="verse">).
     */
    function getCleanTextNodes(element) {
        var textNodes = [];
        if (!element) return textNodes;
        var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, {
            acceptNode: function(node) {
                if (node.parentElement && node.parentElement.classList.contains('verse')) {
                    return NodeFilter.FILTER_REJECT;
                }
                return NodeFilter.FILTER_ACCEPT;
            }
        });
        var node;
        while ((node = walker.nextNode())) {
            textNodes.push(node);
        }
        return textNodes;
    }

    /**
     * Resolves a DOM node and DOM offset to verse number and clean character offset.
     */
    function resolveNodeToVerseOffset(node, domOffset) {
        var parentEl = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
        var verseEl = parentEl ? parentEl.closest('[id^="verse-"]') : null;
        if (!verseEl) return null;

        var verseNum = parseInt(verseEl.id.replace('verse-', ''), 10);
        var textNodes = getCleanTextNodes(verseEl);
        var charOffset = 0;

        for (var i = 0; i < textNodes.length; i++) {
            var tn = textNodes[i];
            if (tn === node) {
                charOffset += domOffset;
                break;
            }
            charOffset += tn.textContent.length;
        }

        return { verse: verseNum, offset: charOffset };
    }

    /**
     * Finds DOM text node and offset inside a verse element for a given clean character offset.
     */
    function findDomPointInVerse(verseElement, targetOffset) {
        var textNodes = getCleanTextNodes(verseElement);
        var currentOffset = 0;

        for (var i = 0; i < textNodes.length; i++) {
            var tn = textNodes[i];
            var len = tn.textContent.length;
            if (currentOffset + len >= targetOffset) {
                return { node: tn, offset: targetOffset - currentOffset };
            }
            currentOffset += len;
        }
        if (textNodes.length > 0) {
            var lastNode = textNodes[textNodes.length - 1];
            return { node: lastNode, offset: lastNode.textContent.length };
        }
        return { node: verseElement, offset: 0 };
    }

    /**
     * Extracts clean text preceding a container node/offset, skipping verse numbers.
     */
    function getCleanTextBefore(containerNode, offset, maxLength) {
        var text = "";
        var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode: function(node) {
                if (node.parentElement && node.parentElement.classList.contains('verse')) {
                    return NodeFilter.FILTER_REJECT;
                }
                return NodeFilter.FILTER_ACCEPT;
            }
        });

        var node;
        while ((node = walker.nextNode())) {
            if (node === containerNode || containerNode.contains(node)) {
                text += node.textContent.substring(0, offset);
                break;
            }
            text += node.textContent;
        }
        return text.slice(-maxLength);
    }

    /**
     * Extracts clean text following a container node/offset, skipping verse numbers.
     */
    function getCleanTextAfter(containerNode, offset, maxLength) {
        var text = "";
        var found = false;
        var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode: function(node) {
                if (node.parentElement && node.parentElement.classList.contains('verse')) {
                    return NodeFilter.FILTER_REJECT;
                }
                return NodeFilter.FILTER_ACCEPT;
            }
        });

        var node;
        while ((node = walker.nextNode())) {
            if (node === containerNode || containerNode.contains(node)) {
                found = true;
                text += node.textContent.substring(offset);
                if (text.length >= maxLength) break;
                continue;
            }
            if (found) {
                text += node.textContent;
                if (text.length >= maxLength) break;
            }
        }
        return text.slice(0, maxLength);
    }

    /**
     * Gets all text nodes enclosed or intersected by a Range (excluding verse numbers).
     */
    function getTextNodesBetween(range) {
        var walker = document.createTreeWalker(range.commonAncestorContainer, NodeFilter.SHOW_TEXT, {
            acceptNode: function(node) {
                if (node.parentElement && node.parentElement.classList.contains('verse')) {
                    return NodeFilter.FILTER_REJECT;
                }
                var nodeRange = document.createRange();
                nodeRange.selectNodeContents(node);

                if (range.compareBoundaryPoints(Range.END_TO_START, nodeRange) < 0 &&
                    range.compareBoundaryPoints(Range.START_TO_END, nodeRange) > 0) {
                    return NodeFilter.FILTER_ACCEPT;
                }
                return NodeFilter.FILTER_REJECT;
            }
        });

        var nodes = [];
        var n;
        while ((n = walker.nextNode())) {
            nodes.push(n);
        }
        return nodes;
    }

    /**
     * Wraps range text nodes into <mark class="aelf-bookmark">.
     */
    function wrapRangeInBookmark(range, bookmarkId) {
        var nodes = getTextNodesBetween(range);
        if (nodes.length === 0) return;

        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var isStart = (node === range.startContainer);
            var isEnd = (node === range.endContainer);
            var startOffset = isStart ? range.startOffset : 0;
            var endOffset = isEnd ? range.endOffset : node.textContent.length;

            var targetNode = node;
            if (isEnd && endOffset < node.textContent.length) {
                node.splitText(endOffset);
            }
            if (isStart && startOffset > 0) {
                targetNode = node.splitText(startOffset);
            }

            var mark = document.createElement('mark');
            mark.className = 'aelf-bookmark';
            mark.dataset.bookmarkId = bookmarkId;

            targetNode.parentNode.insertBefore(mark, targetNode);
            mark.appendChild(targetNode);

            (function(id) {
                var handled = false;
                function onActivate(e) {
                    if (handled) return;
                    handled = true;
                    e.preventDefault();
                    e.stopPropagation();

                    restoreSelectionForBookmark(id);

                    setTimeout(function() {
                        handled = false;
                    }, 400);
                }

                mark.addEventListener('touchstart', onActivate, { passive: false });
                mark.addEventListener('mousedown', onActivate);
                mark.addEventListener('click', onActivate);
            })(bookmarkId);
        }
    }

    /**
     * Restores text selection for a bookmark.
     */
    function restoreSelectionForBookmark(bookmarkId) {
        var marks = Array.from(document.querySelectorAll('mark.aelf-bookmark[data-bookmark-id="' + bookmarkId + '"]'));
        if (marks.length === 0) return;

        var editRange = document.createRange();
        editRange.setStartBefore(marks[0]);
        editRange.setEndAfter(marks[marks.length - 1]);

        var sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(editRange);

        marks.forEach(function(mark) {
            mark.style.backgroundColor = 'transparent';
            mark.style.pointerEvents = 'none';
        });

        setTimeout(function() {
            marks.forEach(function(mark) {
                var parent = mark.parentNode;
                if (parent) {
                    while (mark.firstChild) {
                        parent.insertBefore(mark.firstChild, mark);
                    }
                    parent.removeChild(mark);
                    parent.normalize();
                }
            });

            var s = window.getSelection();
            s.removeAllRanges();
            s.addRange(editRange);
        }, 150);

        if (window.AndroidBridge && window.AndroidBridge.logMessage) {
            window.AndroidBridge.logMessage("Restored selection for bookmark: " + bookmarkId);
        }
    }

    /**
     * Unwraps and removes a bookmark from DOM.
     */
    function removeBookmarkFromDom(bookmarkId) {
        var marks = Array.from(document.querySelectorAll('mark.aelf-bookmark[data-bookmark-id="' + bookmarkId + '"]'));
        marks.forEach(function(mark) {
            var parent = mark.parentNode;
            if (parent) {
                while (mark.firstChild) {
                    parent.insertBefore(mark.firstChild, mark);
                }
                parent.removeChild(mark);
                parent.normalize();
            }
        });
    }

    /**
     * Renders a single bookmark object.
     */
    function renderBookmark(bm) {
        if (!bm || !bm.range) return;
        var startVerseEl = document.getElementById('verse-' + bm.range.startVerse);
        var endVerseEl = document.getElementById('verse-' + bm.range.endVerse);
        if (!startVerseEl || !endVerseEl) return;

        var startPt = findDomPointInVerse(startVerseEl, bm.range.startOffset);
        var endPt = findDomPointInVerse(endVerseEl, bm.range.endOffset);

        if (!startPt || !endPt) return;

        var range = document.createRange();
        range.setStart(startPt.node, startPt.offset);
        range.setEnd(endPt.node, endPt.offset);

        wrapRangeInBookmark(range, bm.id);
    }

    /**
     * Renders an array of bookmarks.
     */
    function renderAllBookmarks(bookmarks) {
        if (!Array.isArray(bookmarks)) return;
        bookmarks.forEach(function(bm) {
            renderBookmark(bm);
        });
    }

    /**
     * Handles selection change and notifies Android bridge.
     */
    function handleSelectionChange() {
        var selection = window.getSelection();
        if (!selection || selection.isCollapsed || selection.rangeCount === 0) {
            return;
        }

        var range = selection.getRangeAt(0);
        var startInfo = resolveNodeToVerseOffset(range.startContainer, range.startOffset);
        var endInfo = resolveNodeToVerseOffset(range.endContainer, range.endOffset);

        if (!startInfo || !endInfo) return;

        var exactText = selection.toString().trim();
        if (!exactText) return;

        var prefix = getCleanTextBefore(range.startContainer, range.startOffset, 30);
        var suffix = getCleanTextAfter(range.endContainer, range.endOffset, 30);

        var bookmarkObj = {
            bookRef: typeof current_book !== 'undefined' ? current_book : "",
            chapterRef: typeof current_chapter !== 'undefined' ? current_chapter : "",
            range: {
                startVerse: startInfo.verse,
                startOffset: startInfo.offset,
                endVerse: endInfo.verse,
                endOffset: endInfo.offset
            },
            textQuote: {
                exact: exactText,
                prefix: prefix,
                suffix: suffix
            }
        };

        if (window.AndroidBridge && window.AndroidBridge.createBookmark) {
            var createdId = window.AndroidBridge.createBookmark(JSON.stringify(bookmarkObj));
            if (createdId) {
                bookmarkObj.id = createdId;
                wrapRangeInBookmark(range, createdId);
            }
        }
    }

    var selectionTimeout;
    document.addEventListener('selectionchange', function() {
        clearTimeout(selectionTimeout);
        selectionTimeout = setTimeout(handleSelectionChange, 300);
    });

    // Public API on window
    window.AelfBookmarks = {
        renderAll: renderAllBookmarks,
        render: renderBookmark,
        remove: function(id) {
            removeBookmarkFromDom(id);
            if (window.AndroidBridge && window.AndroidBridge.deleteBookmark) {
                window.AndroidBridge.deleteBookmark(id);
            }
        }
    };

    // Auto-render initial bookmarks if present
    function init() {
        if (typeof window.current_bookmarks !== 'undefined' && Array.isArray(window.current_bookmarks)) {
            renderAllBookmarks(window.current_bookmarks);
        }
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        setTimeout(init, 50);
    } else {
        document.addEventListener('DOMContentLoaded', function() {
            setTimeout(init, 50);
        });
    }

})();
