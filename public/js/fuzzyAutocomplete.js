(function($){
    $(function(){

        console.log("overwriting autocomplete");

        $.fn.autocomplete = function (options) {
            // Defaults
            var defaults = {
                data: {},
                limit: Infinity,
                onAutocomplete: null,
                triggerOnSingleChoice: false,
                matcher: function(value,list) { //basic startsWith matcher
                    return list.filter(function(el) {
                        return el.toLowerCase().startsWith(value.toLowerCase());
                    });
                }
            };

            options = $.extend(defaults, options);

            return this.each(function() {
                var $input = $(this);
                var data = options.data,
                    matcher = options.matcher,
                $inputDiv = $input.closest('.input-field'); // Div to append on

                $input.click(function (e) {
                    e.preventDefault();
                    $input.val("");
                });

                // Check if data isn't empty
                if (!$.isEmptyObject(data)) {
                    var $autocomplete = $('<ul class="autocomplete-content dropdown-content"></ul>');
                    var $oldAutocomplete;

                    // Append autocomplete element.
                    // Prevent double structure init.
                    if ($inputDiv.length) {
                        $oldAutocomplete = $inputDiv.children('.autocomplete-content.dropdown-content').first();
                        if (!$oldAutocomplete.length) {
                            $inputDiv.append($autocomplete); // Set ul in body
                        }
                    } else {
                        $oldAutocomplete = $input.next('.autocomplete-content.dropdown-content');
                        if (!$oldAutocomplete.length) {
                            $input.after($autocomplete);
                        }
                    }
                    if ($oldAutocomplete.length) {
                        $autocomplete = $oldAutocomplete;
                    }

                    function triggerAutocomplete(text) {
                        $input.val(text);
                        $input.trigger('change');
                        $autocomplete.empty();

                        // Handle onAutocomplete callback.
                        if (typeof(options.onAutocomplete) === "function") {
                            options.onAutocomplete.call(this, text);
                        }

                        $input.trigger('blur');
                    }


                    // Perform search
                    $input.off('keyup.autocomplete').on('keyup.autocomplete', function (e) {

                        // Capture enter.
                        if (e.which === 13) {
                            $autocomplete.find('li').first().click();
                            return;
                        }

                        var val = $input.val().toLowerCase();
                        $autocomplete.empty();

                        // Check if the input isn't empty
                        if (val !== '') {
                            var suggestions = matcher(val, Object.keys(options.data));
                            var cappedSuggestions = suggestions.splice (0,options.limit);
                            cappedSuggestions.forEach(function (suggestion) {
                                var autocompleteOption = $('<li></li>');
                                autocompleteOption.append('<span>'+ suggestion +'</span>');
                                $autocomplete.append(autocompleteOption);
                            });

                            if (options.triggerOnSingleChoice && cappedSuggestions.length == 1) {
                                triggerAutocomplete(cappedSuggestions[0]);
                            }
                        }
                    });

                    // Set input value
                    $autocomplete.on('click', 'li', function () {
                        var text = $(this).text().trim();
                        triggerAutocomplete(text);
                    });
                }
            });
        };
    }); // end of document ready
})(jQuery); // end of jQuery name space
