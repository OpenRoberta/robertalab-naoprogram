#!/bin/bash

version="1.0.7"
dir="$( pwd )"

echo "[Desktop Entry]" > "/usr/share/applications/ORUSBNao.desktop"
echo "Version=$version" >> "/usr/share/applications/ORUSBNao.desktop"
echo "Name=Open Roberta Nao USB" >> "/usr/share/applications/ORUSBNao.desktop"
echo "Exec=java -jar $dir/OpenRobertaNAO-$version.jar" >> "/usr/share/applications/ORUSBNao.desktop"
echo "Path=$dir" >> "/usr/share/applications/ORUSBNao.desktop"
echo "Icon=$dir/OR.png" >> "/usr/share/applications/ORUSBNao.desktop"
echo "Terminal=false" >> "/usr/share/applications/ORUSBNao.desktop"
echo "Type=Application" >> "/usr/share/applications/ORUSBNao.desktop"
echo "Categories=Application;Development;" >> "/usr/share/applications/ORUSBNao.desktop"


chmod u+x "/usr/share/applications/ORUSBNao.desktop"
