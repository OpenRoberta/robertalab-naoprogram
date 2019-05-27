"%WIX%bin\candle.exe" setup.wxs
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupDE_1.0.6.msi -ext WixUIExtension -cultures:de-DE setup.wixobj
"%WIX%bin\light.exe" -out OpenRobertaNAOSetupEN_1.0.6.msi -ext WixUIExtension -cultures:en-US setup.wixobj
@pause
