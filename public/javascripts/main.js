var KEY_S = 83;
var KEY_H = 72;
var KEY_C = 67;
var KEY_ESC = 27;
var KEY_ENTER = 13;
var KEY_PLUS = 61;
var KEY_MINUS = 173;
var CATEGORY_STRING = null;
var SORT_STRING = null;

function shouldExecuteHotkey(event) {
    return !event.shiftKey && !event.altKey && !event.ctrlKey && !event.metaKey;
}

function decodeHtml(html) {
    // lol
    return $('<textarea>').html(html).val();
}

function go(str) {
    window.location = decodeHtml(str);
}

function clearUnread(e) {
    e.find('.unread').remove();
    if (!$('.user-dropdown .unread').length) $('.unread').remove();
}

$(function() {
    $('.alert-fade').fadeIn('slow');

    $('[data-toggle="tooltip"]').tooltip({
        container: "body",
        delay: { "show": 500 }
    });

    $('.authors-icon').click(function() { window.location = '/authors'; });
    
    $('.btn-spinner').click(function() {
        var iconClass = $(this).data('icon');
        $(this).find('.' + iconClass).removeClass(iconClass).addClass('fa-spinner fa-spin');
    });

    var spongie = $('.col-spongie').find('div');
    spongie.click(function() { window.location = 'https://spongepowered.org' });
    spongie
        .mouseenter(function() { $('#spongie').find('path').css('fill', '#F7CF0D'); })
        .mouseleave(function() { $('#spongie').find('path').css('fill', 'gray'); });

    $('.search-icon').click(function() {
        var searchBar = $('.project-search');
        var input = searchBar.find('.input-group');
        if (input.is(':visible')) {
            searchBar.animate({width: '0'}, 100);
            input.fadeOut(100);
        } else {
            var startPos = searchBar.position();
            var dropdown = $('#sp-logo-container');
            var endPos = dropdown.position();
            var a = startPos.left - (endPos.left + dropdown.width());
            var b = startPos.top - endPos.top;
            var distance = Math.sqrt(a*a + b*b);
            input.fadeIn(100);
            searchBar.animate({width: distance}, 100);
            input.find('input').focus();
        }
    });

    var searchBar = $('.project-search');
    searchBar.find('input').on('keypress', function(event) {
        if (event.keyCode === KEY_ENTER) {
            event.preventDefault();
            $(this).next().find('.btn').click();
        }
    });

    searchBar.find('.btn').click(function() {
        var query = $(this).closest('.input-group').find('input').val();
        var url = '/?q=' + query;
        if (CATEGORY_STRING) url += '&categories=' + CATEGORY_STRING;
        if (SORT_STRING) url += '&sort=' + SORT_STRING;
        go(url);
    });

    /*
     * Ore hotkeys:
     *
     * s    Open search
     * esc  Close search
     * h    Go home
     * c    Create new project
     */
    var body = $('body');
    body.keydown(function(event) {
        var target = $(event.target);
        var searchIcon = $('.search-icon');
        if (shouldExecuteHotkey(event)) {
            if (target.is('body')) {
                switch (event.keyCode) {
                    case KEY_S:
                        event.preventDefault();
                        searchIcon.click();
                        break;
                    case KEY_H:
                        event.preventDefault();
                        window.location = '/';
                        break;
                    case KEY_C:
                        event.preventDefault();
                        window.location = '/new';
                        break;
                    case KEY_PLUS:
                        break;
                    case KEY_MINUS:
                        break;
                    default:
                        break;
                }
            } else if (target.is('.project-search input')) {
                switch (event.keyCode) {
                    case KEY_ESC:
                        event.preventDefault();
                        searchIcon.click();
                        break;
                    default:
                        break;
                }
            }
        }
    });
});
