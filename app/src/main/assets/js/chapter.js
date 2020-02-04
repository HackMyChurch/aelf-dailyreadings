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
