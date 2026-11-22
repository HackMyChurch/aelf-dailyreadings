/**
 * AELF Bible Bookmarks & Highlights module
 */
(function() {
    'use strict';

    var activeBookmarksMap = {};
    var currentEditingBookmarkId = null;
    var editingBookmarkObj = null;
    var lastSelectedRange = null;

    function log(msg) {}

    /**
     * Position comparison & overlap utilities.
     */
    function comparePositions(v1, o1, v2, o2) {
        if (v1 !== v2) return v1 - v2;
        return o1 - o2;
    }

    function minPosition(v1, o1, v2, o2) {
        return comparePositions(v1, o1, v2, o2) <= 0 ? { verse: v1, offset: o1 } : { verse: v2, offset: o2 };
    }

    function maxPosition(v1, o1, v2, o2) {
        return comparePositions(v1, o1, v2, o2) >= 0 ? { verse: v1, offset: o1 } : { verse: v2, offset: o2 };
    }

    function rangesOverlap(r1, r2) {
        var maxStart = maxPosition(r1.startVerse, r1.startOffset, r2.startVerse, r2.startOffset);
        var minEnd = minPosition(r1.endVerse, r1.endOffset, r2.endVerse, r2.endOffset);
        return comparePositions(maxStart.verse, maxStart.offset, minEnd.verse, minEnd.offset) <= 0;
    }

    function getOverlappingBookmarks(range) {
        var result = [];
        for (var id in activeBookmarksMap) {
            if (Object.prototype.hasOwnProperty.call(activeBookmarksMap, id)) {
                var bm = activeBookmarksMap[id];
                if (bm && bm.range && rangesOverlap(range, bm.range)) {
                    result.push(bm);
                }
            }
        }
        return result;
    }

    /**
     * Returns clean text nodes inside an element, ignoring verse numbers (<span class="verse">).
     */
    function getCleanTextNodes(element) {
        var textNodes = [];
        if (!element) return textNodes;
        var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, {
            acceptNode: function(node) {
                if (node.parentElement && node.parentElement.closest('.verse')) {
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
     * Finds the closest verse element for any DOM node (text, element, or boundary).
     */
    function findClosestVerseElement(node) {
        if (!node) return null;
        var el = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
        if (!el) return null;

        var v = el.closest('[id^="verse-"]');
        if (v) return v;

        var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_ELEMENT, null);
        walker.currentNode = el;
        var prev = walker.previousNode();
        while (prev) {
            if (prev.id && prev.id.indexOf('verse-') === 0) {
                return prev;
            }
            if (prev.closest) {
                var c = prev.closest('[id^="verse-"]');
                if (c) return c;
            }
            prev = walker.previousNode();
        }

        var allVerses = document.querySelectorAll('[id^="verse-"]');
        if (allVerses.length > 0) return allVerses[0];
        return null;
    }

    /**
     * Resolves a DOM node and DOM offset to verse number and clean character offset.
     * Uses Range.cloneContents() to accurately count clean characters preceding (node, domOffset).
     */
    function resolveNodeToVerseOffset(node, domOffset) {
        if (!node) {
            log("resolveNodeToVerseOffset: node is null");
            return null;
        }

        var verseEl = findClosestVerseElement(node);
        if (!verseEl) {
            log("resolveNodeToVerseOffset: verseEl is null for node " + node.nodeName);
            return null;
        }

        var verseNum = parseInt(verseEl.id.replace('verse-', ''), 10);

        try {
            var r = document.createRange();
            r.setStart(verseEl, 0);

            if (node.nodeType === Node.TEXT_NODE) {
                var safeOffset = Math.min(domOffset, node.textContent.length);
                r.setEnd(node, safeOffset);
            } else if (node.nodeType === Node.ELEMENT_NODE) {
                var safeOffset = Math.min(domOffset, node.childNodes.length);
                r.setEnd(node, safeOffset);
            } else {
                r.setEndAfter(node);
            }

            var frag = r.cloneContents();
            var verseSpans = frag.querySelectorAll('.verse');
            for (var i = 0; i < verseSpans.length; i++) {
                verseSpans[i].remove();
            }

            var charOffset = frag.textContent.length;
            var res = { verse: verseNum, offset: charOffset };
            return res;
        } catch (e) {
            log("resolveNodeToVerseOffset EXCEPTION: " + e);
            return null;
        }
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
                if (node.parentElement && node.parentElement.closest('.verse')) {
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
                if (node.parentElement && node.parentElement.closest('.verse')) {
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
     * Calculates exact selected character range [startOffset, endOffset] inside a TEXT_NODE for any Range.
     */
    function getNodeSelectionRange(node, range) {
        if (!node || node.nodeType !== Node.TEXT_NODE) return null;

        var nodeRange = document.createRange();
        nodeRange.selectNodeContents(node);

        if (range.compareBoundaryPoints(Range.END_TO_START, nodeRange) >= 0 ||
            range.compareBoundaryPoints(Range.START_TO_END, nodeRange) <= 0) {
            return null;
        }

        var startOffset = 0;
        if (range.compareBoundaryPoints(Range.START_TO_START, nodeRange) > 0) {
            if (range.startContainer === node) {
                startOffset = Math.min(range.startOffset, node.textContent.length);
            } else {
                try {
                    var rStart = document.createRange();
                    rStart.setStart(node, 0);
                    rStart.setEnd(range.startContainer, range.startOffset);
                    startOffset = Math.min(rStart.toString().length, node.textContent.length);
                } catch (e) {
                    startOffset = 0;
                }
            }
        }

        var endOffset = node.textContent.length;
        if (range.compareBoundaryPoints(Range.END_TO_END, nodeRange) < 0) {
            if (range.endContainer === node) {
                endOffset = Math.min(range.endOffset, node.textContent.length);
            } else {
                try {
                    var rEnd = document.createRange();
                    rEnd.setStart(node, 0);
                    rEnd.setEnd(range.endContainer, range.endOffset);
                    endOffset = Math.min(rEnd.toString().length, node.textContent.length);
                } catch (e) {
                    endOffset = node.textContent.length;
                }
            }
        }

        return { startOffset: startOffset, endOffset: endOffset };
    }

    /**
     * Gets all text nodes enclosed or intersected by a Range (excluding verse numbers).
     */
    function getTextNodesBetween(range) {
        var root = range.commonAncestorContainer;
        if (root && root.nodeType === Node.TEXT_NODE) {
            root = root.parentNode;
        }
        if (!root) return [];

        var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
            acceptNode: function(node) {
                if (node.parentElement && node.parentElement.closest('.verse')) {
                    return NodeFilter.FILTER_REJECT;
                }
                var bounds = getNodeSelectionRange(node, range);
                if (bounds && bounds.startOffset < bounds.endOffset) {
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
     * Extracts clean text inside a Range.
     */
    function getCleanTextForRange(range) {
        var nodes = getTextNodesBetween(range);
        if (nodes.length === 0) return "";
        var parts = [];
        var currentVerse = findClosestVerseElement(nodes[0]);
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var bounds = getNodeSelectionRange(node, range);
            var newVerse = findClosestVerseElement(node);

            if (currentVerse !== newVerse) {
                // Ensure there is a blank at verse boundaries
                parts.push(" ");
            }

            if (bounds && bounds.startOffset < bounds.endOffset) {
                parts.push(node.textContent.substring(bounds.startOffset, bounds.endOffset));
            }

            currentVerse = newVerse;
        }
        return parts.join("").trim();
    }

    /**
     * Wraps range text nodes into <mark class="aelf-bookmark">.
     */
    function wrapRangeInBookmark(range, bookmarkId) {
        var nodes = getTextNodesBetween(range);
        if (nodes.length === 0) return;

        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var bounds = getNodeSelectionRange(node, range);
            if (!bounds || bounds.startOffset >= bounds.endOffset) continue;

            var startOffset = bounds.startOffset;
            var endOffset = bounds.endOffset;

            var targetNode = node;
            if (endOffset < targetNode.textContent.length) {
                targetNode.splitText(endOffset);
            }
            if (startOffset > 0) {
                targetNode = targetNode.splitText(startOffset);
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
     * Restores text selection for a bookmark and converts it synchronously to a live DOM text selection.
     */
    function restoreSelectionForBookmark(bookmarkId) {
        log("restoreSelectionForBookmark: " + bookmarkId);
        var bm = activeBookmarksMap[bookmarkId];
        if (!bm) return;

        editingBookmarkObj = bm;
        currentEditingBookmarkId = bookmarkId;

        var marks = Array.from(document.querySelectorAll('mark.aelf-bookmark[data-bookmark-id="' + bookmarkId + '"]'));
        if (marks.length === 0) return;

        var firstTextNode = null;
        var lastTextNode = null;

        // Unwrap marks synchronously into plain text nodes FIRST
        marks.forEach(function(mark) {
            var parent = mark.parentNode;
            if (parent) {
                while (mark.firstChild) {
                    var child = mark.firstChild;
                    if (!firstTextNode && child.nodeType === Node.TEXT_NODE) {
                        firstTextNode = child;
                    }
                    if (child.nodeType === Node.TEXT_NODE) {
                        lastTextNode = child;
                    }
                    parent.insertBefore(child, mark);
                }
                parent.removeChild(mark);
            }
        });

        if (!firstTextNode || !lastTextNode) return;

        var editRange = document.createRange();
        editRange.setStart(firstTextNode, 0);
        editRange.setEnd(lastTextNode, lastTextNode.textContent.length);
        lastSelectedRange = editRange.cloneRange();

        // Apply text selection immediately on the 1st tap
        var sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(editRange);

        if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
            window.AndroidBridge.onSelectionStateChanged(true, bookmarkId);
        }
    }

    /**
     * Cancels editing mode and restores the original bookmark highlight if unedited.
     */
    function cancelEditing(force) {
        var selection = window.getSelection();
        log("cancelEditing: force=" + force + ", isCollapsed=" + (selection ? selection.isCollapsed : "no sel") + ", editingObj=" + (editingBookmarkObj ? editingBookmarkObj.id : "none"));
        if (!force && selection && !selection.isCollapsed && selection.rangeCount > 0) {
            if (editingBookmarkObj) {
                currentEditingBookmarkId = editingBookmarkObj.id;
                if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
                    window.AndroidBridge.onSelectionStateChanged(true, editingBookmarkObj.id);
                }
            }
            return;
        }

        if (editingBookmarkObj) {
            renderBookmark(editingBookmarkObj);
            editingBookmarkObj = null;
        }
        currentEditingBookmarkId = null;
        if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
            window.AndroidBridge.onSelectionStateChanged(false, "");
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
        if (!bm || !bm.range || !bm.id) return;
        activeBookmarksMap[bm.id] = bm;

        var startVerseNum = bm.range.startVerse || 1;
        var endVerseNum = bm.range.endVerse || startVerseNum;
        if (endVerseNum < startVerseNum) endVerseNum = startVerseNum;

        var startVerseEl = document.getElementById('verse-' + startVerseNum);
        var endVerseEl = document.getElementById('verse-' + endVerseNum) || startVerseEl;
        if (!startVerseEl) return;
        if (!endVerseEl) endVerseEl = startVerseEl;

        var startPt = findDomPointInVerse(startVerseEl, bm.range.startOffset || 0);
        var endPt = findDomPointInVerse(endVerseEl, bm.range.endOffset || 0);

        if (!startPt) return;
        if (!endPt) endPt = startPt;

        try {
            var range = document.createRange();
            range.setStart(startPt.node, startPt.offset);
            range.setEnd(endPt.node, endPt.offset);

            wrapRangeInBookmark(range, bm.id);
        } catch (e) {
            log("renderBookmark EXCEPTION: " + e);
        }
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
     * Handles selection change and notifies Android bridge of selection state.
     */
    function handleSelectionChange() {
        var selection = window.getSelection();
        log("handleSelectionChange: isCollapsed=" + (selection ? selection.isCollapsed : "no sel") + ", editingObj=" + (editingBookmarkObj ? editingBookmarkObj.id : "none"));

        if (selection && !selection.isCollapsed && selection.rangeCount > 0) {
            lastSelectedRange = selection.getRangeAt(0).cloneRange();
        }

        // While editing a bookmark, maintain edit mode state
        if (editingBookmarkObj) {
            if (selection && !selection.isCollapsed && selection.rangeCount > 0) {
                currentEditingBookmarkId = editingBookmarkObj.id;
                if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
                    window.AndroidBridge.onSelectionStateChanged(true, editingBookmarkObj.id);
                }
            }
            return;
        }

        if (!selection || selection.isCollapsed || selection.rangeCount === 0) {
            if (currentEditingBookmarkId) {
                currentEditingBookmarkId = null;
                if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
                    window.AndroidBridge.onSelectionStateChanged(false, "");
                }
            }
            return;
        }

        var range = selection.getRangeAt(0);
        var startInfo = resolveNodeToVerseOffset(range.startContainer, range.startOffset);
        var endInfo = resolveNodeToVerseOffset(range.endContainer, range.endOffset);

        if (!startInfo || !endInfo) return;

        var cand = {
            startVerse: startInfo.verse,
            startOffset: startInfo.offset,
            endVerse: endInfo.verse,
            endOffset: endInfo.offset
        };

        var overlaps = getOverlappingBookmarks(cand);
        if (overlaps.length > 0) {
            currentEditingBookmarkId = overlaps[0].id;
            if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
                window.AndroidBridge.onSelectionStateChanged(true, overlaps[0].id);
            }
        } else {
            currentEditingBookmarkId = null;
            if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
                window.AndroidBridge.onSelectionStateChanged(false, "");
            }
        }
    }

    var selectionTimeout;
    document.addEventListener('selectionchange', function() {
        var selection = window.getSelection();
        if (selection && !selection.isCollapsed && selection.rangeCount > 0) {
            lastSelectedRange = selection.getRangeAt(0).cloneRange();
        }
        clearTimeout(selectionTimeout);
        selectionTimeout = setTimeout(handleSelectionChange, 200);
    });

    /**
     * Saves the current text selection as a bookmark, merging any overlapping bookmarks.
     */
    function saveCurrentSelection() {
        log("saveCurrentSelection START");
        var selection = window.getSelection();
        var range = null;
        if (selection && !selection.isCollapsed && selection.rangeCount > 0) {
            range = selection.getRangeAt(0);
            lastSelectedRange = range.cloneRange();
        } else if (lastSelectedRange) {
            range = lastSelectedRange;
        }

        if (!range) {
            log("saveCurrentSelection ABORT: no active selection (isCollapsed=" + (selection ? selection.isCollapsed : "null") + ") and no lastSelectedRange");
            return;
        }
        log("saveCurrentSelection: range.startContainer=" + range.startContainer.nodeName + " (" + range.startContainer.nodeType + "), startOffset=" + range.startOffset + ", endContainer=" + range.endContainer.nodeName + " (" + range.endContainer.nodeType + "), endOffset=" + range.endOffset);

        var startInfo = resolveNodeToVerseOffset(range.startContainer, range.startOffset);
        var endInfo = resolveNodeToVerseOffset(range.endContainer, range.endOffset);

        log("saveCurrentSelection: startInfo=" + JSON.stringify(startInfo) + ", endInfo=" + JSON.stringify(endInfo));
        if (!startInfo || !endInfo) {
            log("saveCurrentSelection ABORT: startInfo or endInfo is null");
            return;
        }

        var mergedRange = {
            startVerse: startInfo.verse,
            startOffset: startInfo.offset,
            endVerse: endInfo.verse,
            endOffset: endInfo.offset
        };

        var targetEditingId = editingBookmarkObj ? editingBookmarkObj.id : null;
        var targetCreatedAt = editingBookmarkObj ? editingBookmarkObj.createdAt : null;

        var overlaps = getOverlappingBookmarks(mergedRange);
        log("saveCurrentSelection: targetEditingId=" + targetEditingId + ", overlapsCount=" + overlaps.length);

        if (overlaps.length > 0) {
            overlaps.forEach(function(bm) {
                if (bm.id !== targetEditingId) {
                    var startP = minPosition(mergedRange.startVerse, mergedRange.startOffset, bm.range.startVerse, bm.range.startOffset);
                    var endP = maxPosition(mergedRange.endVerse, mergedRange.endOffset, bm.range.endVerse, bm.range.endOffset);

                    mergedRange.startVerse = startP.verse;
                    mergedRange.startOffset = startP.offset;
                    mergedRange.endVerse = endP.verse;
                    mergedRange.endOffset = endP.offset;

                    removeBookmarkFromDom(bm.id);
                    delete activeBookmarksMap[bm.id];
                    if (window.AndroidBridge && window.AndroidBridge.deleteBookmark) {
                        window.AndroidBridge.deleteBookmark(bm.id);
                    }
                }
            });
        }

        if (editingBookmarkObj && activeBookmarksMap[editingBookmarkObj.id]) {
            removeBookmarkFromDom(editingBookmarkObj.id);
            delete activeBookmarksMap[editingBookmarkObj.id];
        }

        var startVerseEl = document.getElementById('verse-' + mergedRange.startVerse);
        var endVerseEl = document.getElementById('verse-' + mergedRange.endVerse);
        if (!startVerseEl || !endVerseEl) {
            log("saveCurrentSelection ABORT: verse element not found for " + mergedRange.startVerse + " or " + mergedRange.endVerse);
            return;
        }

        var startPt = findDomPointInVerse(startVerseEl, mergedRange.startOffset);
        var endPt = findDomPointInVerse(endVerseEl, mergedRange.endOffset);

        if (!startPt || !endPt) {
            log("saveCurrentSelection ABORT: startPt or endPt is null");
            return;
        }

        var domRange = document.createRange();
        domRange.setStart(startPt.node, startPt.offset);
        domRange.setEnd(endPt.node, endPt.offset);

        var exactText = getCleanTextForRange(domRange);
        log("saveCurrentSelection: exactText='" + exactText + "'");

        if (!exactText) {
            log("saveCurrentSelection ABORT: exactText is empty");
            return;
        }

        var prefix = getCleanTextBefore(domRange.startContainer, domRange.startOffset, 30);
        var suffix = getCleanTextAfter(domRange.endContainer, domRange.endOffset, 30);

        var bookmarkObj = {
            id: targetEditingId || undefined,
            bookRef: typeof current_book !== 'undefined' ? current_book : "",
            chapterRef: typeof current_chapter !== 'undefined' ? current_chapter : "",
            range: {
                startVerse: mergedRange.startVerse,
                startOffset: mergedRange.startOffset,
                endVerse: mergedRange.endVerse,
                endOffset: mergedRange.endOffset
            },
            textQuote: {
                exact: exactText,
                prefix: prefix,
                suffix: suffix
            },
            createdAt: targetCreatedAt || undefined
        };

        if (window.AndroidBridge && window.AndroidBridge.createBookmark) {
            var createdId = window.AndroidBridge.createBookmark(JSON.stringify(bookmarkObj));
            log("saveCurrentSelection: createBookmark returned id=" + createdId);
            if (createdId) {
                bookmarkObj.id = createdId;
                activeBookmarksMap[createdId] = bookmarkObj;
                wrapRangeInBookmark(domRange, createdId);
            }
        }

        selection.removeAllRanges();
        editingBookmarkObj = null;
        currentEditingBookmarkId = null;
        if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
            window.AndroidBridge.onSelectionStateChanged(false, "");
        }
        log("saveCurrentSelection COMPLETE");
    }

    /**
     * Deletes the currently selected or edited bookmark.
     */
    function deleteCurrentBookmark() {
        var targetId = currentEditingBookmarkId;
        if (!targetId && editingBookmarkObj) {
            targetId = editingBookmarkObj.id;
        }

        if (!targetId) {
            var selection = window.getSelection();
            if (selection && !selection.isCollapsed && selection.rangeCount > 0) {
                var range = selection.getRangeAt(0);
                var startInfo = resolveNodeToVerseOffset(range.startContainer, range.startOffset);
                var endInfo = resolveNodeToVerseOffset(range.endContainer, range.endOffset);
                if (startInfo && endInfo) {
                    var cand = {
                        startVerse: startInfo.verse,
                        startOffset: startInfo.offset,
                        endVerse: endInfo.verse,
                        endOffset: endInfo.offset
                    };
                    var overlaps = getOverlappingBookmarks(cand);
                    if (overlaps.length > 0) {
                        targetId = overlaps[0].id;
                    }
                }
            }
        }

        if (targetId) {
            removeBookmarkFromDom(targetId);
            delete activeBookmarksMap[targetId];
            if (window.AndroidBridge && window.AndroidBridge.deleteBookmark) {
                window.AndroidBridge.deleteBookmark(targetId);
            }
        }

        var selectionObj = window.getSelection();
        if (selectionObj) selectionObj.removeAllRanges();

        editingBookmarkObj = null;
        currentEditingBookmarkId = null;
        if (window.AndroidBridge && window.AndroidBridge.onSelectionStateChanged) {
            window.AndroidBridge.onSelectionStateChanged(false, "");
        }
    }

    /**
     * Highlights and scrolls to the targeted bookmark, flashing it lightly.
     */
    function highlightTargetBookmark(bookmarkId) {
        if (!bookmarkId) {
            document.documentElement.classList.remove('target-pending');
            return;
        }
        var marks = document.querySelectorAll('mark.aelf-bookmark[data-bookmark-id="' + bookmarkId + '"]');
        if (marks && marks.length > 0) {
            try {
                marks[0].scrollIntoView({ block: 'center' });
            } catch (e) {
                marks[0].scrollIntoView();
            }

            // Reveal body on frame 1 directly at the target position
            document.documentElement.classList.remove('target-pending');

            // Start background pulse after revealing
            requestAnimationFrame(function() {
                for (var i = 0; i < marks.length; i++) {
                    marks[i].classList.add('flash');
                }
            });

            setTimeout(function() {
                for (var i = 0; i < marks.length; i++) {
                    marks[i].classList.remove('flash');
                }
            }, 4000);
        } else {
            document.documentElement.classList.remove('target-pending');
        }
    }

    // Public API on window
    window.AelfBookmarks = {
        renderAll: renderAllBookmarks,
        render: renderBookmark,
        highlightTargetBookmark: highlightTargetBookmark,
        remove: function(id) {
            removeBookmarkFromDom(id);
            delete activeBookmarksMap[id];
            if (window.AndroidBridge && window.AndroidBridge.deleteBookmark) {
                window.AndroidBridge.deleteBookmark(id);
            }
        },
        saveCurrentSelection: saveCurrentSelection,
        deleteCurrentBookmark: deleteCurrentBookmark,
        cancelEditing: cancelEditing
    };

    // Auto-render initial bookmarks if present
    function init() {
        if (typeof window.current_bookmarks !== 'undefined' && Array.isArray(window.current_bookmarks)) {
            renderAllBookmarks(window.current_bookmarks);
        }
        if (typeof window.target_bookmark !== 'undefined' && window.target_bookmark) {
            highlightTargetBookmark(window.target_bookmark);
        } else {
            document.documentElement.classList.remove('target-pending');
        }
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        init();
    } else {
        document.addEventListener('DOMContentLoaded', function() {
            init();
        });
    }

    // Safety fallback: ensure target-pending class is removed
    setTimeout(function() {
        document.documentElement.classList.remove('target-pending');
    }, 500);

})();
