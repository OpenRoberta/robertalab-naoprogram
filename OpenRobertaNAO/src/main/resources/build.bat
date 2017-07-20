"%WIX%bin\candle.exe" setup.wxs
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupDE_${project.version}.msi -ext WixUIExtension -cultures:de-DE setup.wixobj
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupEN_${project.version}.msi -ext WixUIExtension -cultures:en-US setup.wixobj
@pause
