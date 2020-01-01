// Highlight search keywords
if (highlight) {
    var instance = new Mark(document.querySelector("body"));
    instance.mark(highlight, {
        "accuracy": "exactly",
        "ignoreJoiners": true,
        "acrossElements": true,
        "ignoreJoiners": true,
        "ignorePunctuation": ":;.,-–—‒_(){}[]!'\"+=".split(""),
        "wildcards": "enabled",
    });
}
