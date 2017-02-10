"%WIX%bin\candle.exe" setup.wxs resources.wxs
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupDE.msi -ext WixUIExtension -cultures:de-DE setup.wixobj resources.wixobj -b ./resources
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupEN.msi -ext WixUIExtension -cultures:en-US setup.wixobj resources.wixobj -b ./resources
@pause