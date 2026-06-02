var SMWP_SEARCH_BUTTON_ID =
    (typeof LinkButton1 !== 'undefined' && LinkButton1.id)
        ? LinkButton1.id
        : 'LinkButton1';

var SMWP_SEARCH_LAST_CLICK_AT = 0;

function smwpSearchNormalizeId(id) {
    if (!id) return '';

    if (id.charAt(0) === '#') {
        return id.substring(1);
    }

    return id;
}

function smwpSearchTodayText() {
    var today = new Date();

    var y = today.getFullYear();
    var m = today.getMonth() + 1;
    var d = today.getDate();

    return y + '-' +
        (m < 10 ? '0' + m : m) + '-' +
        (d < 10 ? '0' + d : d);
}

function smwpSearchDateboxId() {
    if (typeof DateBox1 !== 'undefined' && DateBox1.id) {
        return smwpSearchNormalizeId(DateBox1.id);
    }

    return 'DateBox1';
}

function smwpSearchDateValue() {
    var dateboxId = smwpSearchDateboxId();
    var value = '';

    try {
        var $datebox = $('#' + dateboxId);

        if ($datebox.length > 0) {
            value = $datebox.datebox('getValue');
        } else {
            console.warn('[SMWP Search] datebox not found:', dateboxId);
        }

    } catch (e) {
        console.warn('[SMWP Search] DateBox getValue failed:', e);
    }

    if (!value) {
        value = smwpSearchTodayText();
    }

    return value;
}

function smwpSearchByButtonClick(event) {
    if (event && event.preventDefault) {
        event.preventDefault();
    }

    var now = new Date().getTime();

    if (now - SMWP_SEARCH_LAST_CLICK_AT < 120) {
        return;
    }

    SMWP_SEARCH_LAST_CLICK_AT = now;

    var dateText = smwpSearchDateValue();

    console.log('[SMWP Search] selected date:', dateText);

    if (typeof smwpSetSearchDate === 'function') {
        smwpSetSearchDate(dateText);
        return;
    }

    if (typeof window.SMWPRefreshEnergyChart === 'function') {
        window.SMWP_SELECTED_DATE = dateText;
        window.SMWPRefreshEnergyChart(dateText);
        return;
    }

    console.error('[SMWP Search] search function is not ready');
}

$(document).ready(function () {
    var buttonId = smwpSearchNormalizeId(SMWP_SEARCH_BUTTON_ID);
    var $button = $('#' + buttonId);

    if (!$button.length) {
        console.warn('[SMWP Search] button not found:', buttonId);
        return;
    }

    $button.off('click.smwpSearch')
           .on('click.smwpSearch', smwpSearchByButtonClick);
});