"%WIX%bin\candle.exe" setup.wxs
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupDE_%VERSION%.msi -ext WixUIExtension -cultures:de-DE setup.wixobj
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupEN_%VERSION%.msi -ext WixUIExtension -cultures:en-US setup.wixobj
@pause
