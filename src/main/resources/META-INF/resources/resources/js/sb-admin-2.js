(function ($) {
    "use strict"; // Start of use strict

    // Toggle the side navigation
    $("#sidebarToggle, #sidebarToggleTop").on('click', function (e) {
        $("body").toggleClass("sidebar-toggled");
        $(".sidebar").toggleClass("toggled");
        if ($(".sidebar").hasClass("toggled")) {
            $('.sidebar .collapse').collapse('hide');
            document.getElementById("logoOpenTheso").style.display = "block";

            var thesoButton1 = document.getElementById("thesoButton1");
            if (thesoButton1 !== null) {
                thesoButton1.style.display = "none";
            }
            var thesoButton2 = document.getElementById("thesoButton2");
            if (thesoButton2 !== null) {
                thesoButton2.style.display = "block";
            }

            var candidatButton1 = document.getElementById("candidatButton1");
            if (candidatButton1 !== null) {
                candidatButton1.style.display = "none";
            }
            var candidatButton2 = document.getElementById("candidatButton2");
            if (candidatButton2 !== null) {
                candidatButton2.style.display = "block";
            }

            var profileButton1 = document.getElementById("profileButton1");
            if (profileButton1 !== null) {
                profileButton1.style.display = "none";
            }
            var profileButton2 = document.getElementById("profileButton2");
            if (profileButton2 !== null) {
                profileButton2.style.display = "block";
            }

            var toolboxButton1 = document.getElementById("toolboxButton1");
            if (toolboxButton1 !== null) {
                toolboxButton1.style.display = "none";
            }
            var toolboxButton2 = document.getElementById("toolboxButton2");
            if (toolboxButton2 !== null) {
                toolboxButton2.style.display = "block";
            }

            var settingButton1 = document.getElementById("settingButton1");
            if (settingButton1 !== null) {
                settingButton1.style.display = "none";
            }
            var settingButton2 = document.getElementById("settingButton2");
            if (settingButton2 !== null) {
                settingButton2.style.display = "block";
            }
            
            var graph1 = document.getElementById("graph1");
            if (graph1 !== null) {
                graph1.style.display = "none";
            }
            var graph2 = document.getElementById("graph2");
            if (graph2 !== null) {
                graph2.style.display = "block";
            }            
        } else {
            document.getElementById("logoOpenTheso").style.display = "none";

            var thesoButton1 = document.getElementById("thesoButton1");
            if (thesoButton1 !== null) {
                thesoButton1.style.display = "block";
            }
            var thesoButton2 = document.getElementById("thesoButton2");
            if (thesoButton2 !== null) {
                thesoButton2.style.display = "none";
            }

            var candidatButton1 = document.getElementById("candidatButton1");
            if (candidatButton1 !== null) {
                candidatButton1.style.display = "block";
            }
            var candidatButton2 = document.getElementById("candidatButton2");
            if (candidatButton2 !== null) {
                candidatButton2.style.display = "none";
            }

            var profileButton1 = document.getElementById("profileButton1");
            if (profileButton1 !== null) {
                profileButton1.style.display = "block";
            }
            var profileButton2 = document.getElementById("profileButton2");
            if (profileButton2 !== null) {
                profileButton2.style.display = "none";
            }

            var toolboxButton1 = document.getElementById("toolboxButton1");
            if (toolboxButton1 !== null) {
                toolboxButton1.style.display = "block";
            }
            var toolboxButton2 = document.getElementById("toolboxButton2");
            if (toolboxButton2 !== null) {
                toolboxButton2.style.display = "none";
            }

            var settingButton1 = document.getElementById("settingButton1");
            if (settingButton1 !== null) {
                settingButton1.style.display = "block";
            }
            var settingButton2 = document.getElementById("settingButton2");
            if (settingButton2 !== null) {
                settingButton2.style.display = "none";
            }
            var graph1 = document.getElementById("graph1");
            if (graph1 !== null) {
                graph1.style.display = "block";
            }
            var graph2 = document.getElementById("graph2");
            if (graph2 !== null) {
                graph2.style.display = "none";
            }            
            
        }
        ;
    });

    // Close any open menu accordions when window is resized below 768px
    $(window).resize(function () {
        if ($(window).width() < 768) {
            $('.sidebar .collapse').collapse('hide');
        }
        ;

        // Toggle the side navigation when window is resized below 480px
        if ($(window).width() < 480 && !$(".sidebar").hasClass("toggled")) {
            $("body").addClass("sidebar-toggled");
            $(".sidebar").addClass("toggled");
            $('.sidebar .collapse').collapse('hide');
        }
        ;
    });

    // Prevent the content wrapper from scrolling when the fixed side navigation hovered over
    $('body.fixed-nav .sidebar').on('mousewheel DOMMouseScroll wheel', function (e) {
        if ($(window).width() > 768) {
            var e0 = e.originalEvent,
                    delta = e0.wheelDelta || -e0.detail;
            this.scrollTop += (delta < 0 ? 1 : -1) * 30;
            e.preventDefault();
        }
    });

    // Scroll to top button appear
    $(document).on('scroll', function () {
        var scrollDistance = $(this).scrollTop();
        if (scrollDistance > 100) {
            $('.scroll-to-top').fadeIn();
        } else {
            $('.scroll-to-top').fadeOut();
        }
    });

    // Smooth scrolling using jQuery easing
    $(document).on('click', 'a.scroll-to-top', function (e) {
        var $anchor = $(this);
        $('html, body').stop().animate({
            scrollTop: ($($anchor.attr('href')).offset().top)
        }, 1000, 'easeInOutExpo');
        e.preventDefault();
    });

})(jQuery); // End of use strict

function closeMenu() {
    $("body").toggleClass("sidebar-toggled");
    $(".sidebar").toggleClass("toggled");
    if ($(".sidebar").hasClass("toggled")) {
        $('.sidebar .collapse').collapse('hide');

        document.getElementById("thesoButton1").style.display = "none";

        var candidatButton1 = document.getElementById("candidatButton1");
        if (candidatButton1 !== null) {
            candidatButton1.style.display = "none";
        }

        var profileButton1 = document.getElementById("profileButton1");
        if (profileButton1 !== null) {
            profileButton1.style.display = "none";
        }

        var toolboxButton1 = document.getElementById("toolboxButton1");
        if (toolboxButton1 !== null) {
            toolboxButton1.style.display = "none";
        }

        var settingButton1 = document.getElementById("settingButton1");
        if (settingButton1 !== null) {
            settingButton1.style.display = "none";
        }
    }
    ;
}

function initMenu() {
    var thesoButton1 = document.getElementById("thesoButton1");
    if (thesoButton1 !== null) {
        thesoButton1.style.display = "none";
    }

    var candidatButton1 = document.getElementById("candidatButton1");
    if (candidatButton1 !== null) {
        candidatButton1.style.display = "none";
    }

    var profileButton1 = document.getElementById("profileButton1");
    if (profileButton1 !== null) {
        profileButton1.style.display = "none";
    }

    var toolboxButton1 = document.getElementById("toolboxButton1");
    if (toolboxButton1 !== null) {
        toolboxButton1.style.display = "none";
    }

    var settingButton1 = document.getElementById("settingButton1");
    if (settingButton1 !== null) {
        settingButton1.style.display = "none";
    }
    
    var graph1 = document.getElementById("graph1");
    if (graph1 !== null) {
        graph1.style.display = "none";
    }    
}