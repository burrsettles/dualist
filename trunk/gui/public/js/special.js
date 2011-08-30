$(document).ready(function () {
	// array used for coloring things in the UI
	var hilite1 = new Array('#d00', '#00e', '#0c0', '#dd0', '#e0e', '#0dd', '#c90', '#90b', '#333', '#ccc');
	var hilite2 = new Array('#fcc', '#ccf', '#cfc', '#ff9', '#f9f', '#9ff', '#fc6', '#d9f', '#999', '#eee');
	hilite1[-1] = '#000';
	hilite2[-1] = '#666';

	// get global time offset for logging
	var starttime = get_time() - $('#timeSoFar').val();

	// determine width of various panes in the interface
	var myLayout = $('body').layout({ applyDefaultStyles: true });
	myLayout.sizePane('north', 60);
	var numLabels = $('.featureQuery').length;
	var w = Math.max( 90, 200 - (20 * Math.max(0, numLabels-2)) );
	var paneWidth = (w+1)*numLabels+2;
	myLayout.sizePane('east', paneWidth);

	// auto-resize the feature-labeling columns
	var h = $('.featurePane').height() - 51;
	$('.featureQuery').width(w);
	$('.featureQuery input').width(w-5);
	$('.featureQuery .queryList').width(w);
	$('.featureQuery .queryList').height( h );
	
	// initialize colors and stuff
	$('.featureQuery').each(function(){
		$(this).css('background-color', hilite1[$(this).attr('labelIndex')]);
		var c = hilite2[$(this).attr('labelIndex')];
		$(this).find('strong').css('color', c);
	});
	$('.feature').live('mouseenter', function(){
		if (! $(this).hasClass('labeled'))
			$(this).css('background-color', hilite2[$(this).attr('labelIndex')]);
	});
	$('.feature').live('mouseleave', function(){
		if (! $(this).hasClass('labeled'))
			$(this).css('background-color', '');
	});
	$('.feature').each(function(){
		if ($(this).hasClass('labeled'))
			$(this).css('background-color', hilite1[$(this).attr('labelIndex')]);
	});
	$('.label').each(function(){
		$(this).css('border-color', hilite1[$(this).attr('labelIndex')]);
		$(this).css('background-color', hilite2[$(this).attr('labelIndex')]);
	});
	$('.label').mouseenter(function(){
		$(this).css('background-color', hilite1[$(this).attr('labelIndex')]);
	});
	$('.label').mouseleave(function(){
		if (! $(this).hasClass('labeled'))
			$(this).css('background-color', hilite2[$(this).attr('labelIndex')]);
	});

	// dynamically-added feature labels from input boxes
	$('.addFeature').keypress(function(e) {
		if((e.which == 13 || e.which == 0) && $(this).val() != '') {
			var time = get_time() - starttime;
			var labelIndex = $(this).attr('labelIndex');
			var features = $(this).attr('value').split(' ');
			var color = hilite1[labelIndex];
			for (var i = features.length-1; i >= 0; i--) {
				var feature = features[i];
				var id = labelIndex + '||' + feature;
				$(this).next('.queryList').prepend( '<div class="feature labeled" style="background-color:'+color+';" labelIndex="'+labelIndex+'" id="'+id+'">' + feature + '</div>' );
				$(this).attr('value', '');
				log_it(time + "\taddFeature\t" + id + "\n");
			}
		}
	});
	
	// toggle a labeled feature on/off
	$('.feature').live('click', function(){ 
		var time = get_time() - starttime;
		if ($(this).hasClass('labeled')) {
			$(this).removeClass('labeled');
			$(this).css('background-color', hilite2[$(this).attr('labelIndex')]);
			log_it(time + "\tunlabelFeature\t" + $(this).attr('id') + "\n");
		}
		else {
			$(this).addClass('labeled');
			$(this).css('background-color', hilite1[$(this).attr('labelIndex')]);
			log_it(time + "\tlabelFeature\t" + $(this).attr('id') + "\n");
		}
	});
	
	// toggle a labeled instance on/off
	$('.label').click(function() {
		var time = get_time() - starttime;
		if ($(this).hasClass('labeled')) {
			$(this).removeClass('labeled');
			$(this).css('background-color', hilite2[$(this).attr('labelIndex')]);
			$(this).closest('.instQuery').css('background-color', '#eee').css('border-color', '');
			// $(this).closest('.serialFeature').css('background-color', '');
			log_it(time + "\tunlabelInstance\t" + $(this).attr('id') + "\n");
		}
		else {
			$(this).siblings().each(function(){
				$(this).removeClass('labeled');
				$(this).css('background-color', hilite2[$(this).attr('labelIndex')]);
			});
			$(this).addClass('labeled');
			$(this).css('background-color', hilite1[$(this).attr('labelIndex')]);
			$(this).closest('.instQuery').css('background-color', hilite2[$(this).attr('labelIndex')]).css('border-color', hilite1[$(this).attr('labelIndex')]);
			// $(this).closest('.serialFeature').css('background-color', '#ccc');
			log_it(time + "\tlabelInstance\t" + $(this).attr('id') + "\n");
		}
	});

	// submit button
	$('#learn').click(function () {
		var features = '';
		var instances = '';
		$('.labeled').each(function() {
			if ($(this).hasClass('feature'))
				features = features + $(this).attr('id') + ' ';
			if ($(this).hasClass('label'))
				instances = instances + $(this).attr('id') + ' ';
		});
		$('#oracleFeatures').val(features);
		$('#oracleInstances').val(instances);
		$('#oracle').submit();
	});

	// prediction button
	$('#predict').click(function () {
		var features = '';
		var instances = '';
		$('.labeled').each(function() {
			if ($(this).hasClass('feature'))
				features = features + $(this).attr('id') + ' ';
			if ($(this).hasClass('label'))
				instances = instances + $(this).attr('id') + ' ';
		});
		$('#trainingFeatures').val(features);
		$('#trainingInstances').val(instances);
		$('#makePredictions').submit();
	});
	
	// alert('all clear!');
	// alert(starttime);
});

// useful for timestamping
function get_time() {
	var now = new Date();
	return Math.round(now.getTime() / 1000);
}

// adds a line of text to the outgoing activity log
function log_it(str) {
	$('#oracleLog').val( $('#oracleLog').val() + str);
}
