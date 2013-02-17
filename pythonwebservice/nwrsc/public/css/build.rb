#!/usr/bin/env ruby

require 'sass'

#options = {}
options = {:style => :compressed}

Sass.compile_file("internal/register.scss", "register.css", options)
Sass.compile_file("internal/admin.scss", "admin.css", options)
Sass.compile_file("internal/announcer.scss", "announcer.css", options)
Sass.compile_file("internal/live.scss", "live.css", options)
Sass.compile_file("internal/history.scss", "history.css", options)

