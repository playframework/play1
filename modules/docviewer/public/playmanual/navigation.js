$(document).ready(function(){

	// Construct an OL of this page's H2
	var navigation = '<ol class="navigation">';
	var h2index = 0;
	var h3index = 0;
	$('#page h2, #page h3').each(function(index) {

		// In each heading, construct an in-page link from its id, or the nested a[name]
		if ($(this).find('a[name]').length == 0) {
			var anchor = $(this).attr('id');
		}
		else {
			var anchor = $(this).find('a[name]').attr('name');
		}
		var link = '<a href="#' + anchor + '">' + $(this).text() + '</a>';

		if (this.tagName == 'H2') {
			// Close a nested OL list for the previous H3.
			if (h3index > 0) {
				navigation += '</ol>';
			}
			h3index = 0;
			
			// Close the LI for the previous H2.
			if (h2index > 0) {
				navigation += '</li>';
			}
			h2index++;

			// Output LI start tag and body for this H2.
			navigation += '<li>' + link;
		}
		
		// Output a nested LI for this H3.
		if (this.tagName == 'H3') {
			h3index++;
			
			// Start a new nested OL for the first H3.
			if (h3index == 1) {
				navigation += '<ol>';
			}
			
			navigation += '<li>' + link + '</li>';
		}
		
	});	
	
	// Close the LI for the last H2, and close the outer list.
	navigation += '</li></ol>';
	$('#toc').html(navigation);
	
	// Next link
	var nextLink = $('.next a')
    if(nextLink && nextLink.size()) {
        $('#gotoc').after('<li><a href="' + $(nextLink).attr('href') + '">Next: ' + $(nextLink).text() + '</a></li>')
    }
});
