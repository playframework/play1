$(document).ready(function(){

	// Construct an OL of this page's H2
	var navigation = '<ol class="navigation">';
	var h2index = 0;
	var h3index = 0;
	
	$('#pageContent h2, #pageContent h3, #pageContent > div > ol > li > a').each(function(index) {
	  
		// In each heading, construct an in-page link from its id, or the nested a[name]
		if ($(this).find('a[name]').length == 0) {
			var anchor = $(this).attr('id');
		}
		else {
			var anchor = $(this).find('a[name]').attr('name');
		}
		var link = '<a href="#' + anchor + '">' + $(this).text() + '</a>';
		if(this.tagName == 'A') {
		    link = '<a href="' + $(this).attr('href') + '">' + $(this).text() + '</a>';
		}

		if (this.tagName == 'H2') {
			// Close a nested OL list for the previous H3.
			if (h3index > 0) {
				navigation += '</ul>';
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
		var linkHasNestedList = (this.tagName == 'A' && $(this).parent('li').find('li').size() > 0);
		if (this.tagName == 'H3' || linkHasNestedList || $(this).hasClass('navigation') ) {
			h3index++;
			
			// Start a new nested OL for the first H3.
			if (h3index == 1) {
				navigation += '<ul>';
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
