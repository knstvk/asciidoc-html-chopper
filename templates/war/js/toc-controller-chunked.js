/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
$(document).ready(function () {
    $(".book").append('<div id="smallDeviceIndicator" style="display: none">');

    // $('#toc .sectlevel1').treeview({
    //     collapsed: true,
    //     animated: "medium",
    //     persist: "location",
    //     unique: false,
    // });

    var closePanel = $("#close-panel");
    var tocMarker = $("#toc-position-marker");

    var book = $(".book");
    var tocPanel = $("#toc");

    closePanel.bind('click', function (e) {
        e.preventDefault();
        tocPanel.addClass('toc-collapsed');
        book.addClass('toc-collapsed');
        tocMarker.addClass('toc-collapsed');
    });

    tocMarker.bind('click', function (e) {
        e.preventDefault();
        tocPanel.removeClass('toc-collapsed');
        book.removeClass('toc-collapsed');
        tocMarker.removeClass('toc-collapsed');
    });
});