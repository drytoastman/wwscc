#!/usr/bin/env ruby

require 'sass'

Sass.compile_file("internal/register.scss", "register.css")
Sass.compile_file("internal/registerold.scss", "registerold.css")
Sass.compile_file("internal/admin.scss", "admin.css")
Sass.compile_file("internal/announcer.scss", "announcer.css")

