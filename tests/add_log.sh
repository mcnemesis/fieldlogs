#!/usr/bin/sh
curl  \
     -F sig=@sig.jpg \
     -F img=@mice.png \
    -F lat="0.56" \
    -F lng="5.2" \
    -F sub="Logging Mice" \
    -F nat="other" \
    -F dob="2000-12-6" \
    -F loc="A Place" \
    -F com="So we hunted the mice" \
    -F code="~UFO-6" \
    -F cap="The big fat thing" \
    http://localhost/logs/api/logs/add