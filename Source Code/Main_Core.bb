Local InitErrorStr$ = ""

If FileSize("fmod.dll")=0 Then InitErrorStr=InitErrorStr+ "fmod.dll"+Chr(13)+Chr(10)
If FileSize("BlitzMovie.dll")=0 Then InitErrorStr=InitErrorStr+ "BlitzMovie.dll"+Chr(13)+Chr(10)
If FileSize("dplayx.dll")=0 Then InitErrorStr=InitErrorStr+ "dplayxdll"+Chr(13)+Chr(10)
If FileSize("FreeImage.dll")=0 Then InitErrorStr=InitErrorStr+ "FreeImage"+Chr(13)+Chr(10)

If Len(InitErrorStr)>0 Then
	RuntimeError "The following DLLs were not found in the game directory:"+Chr(13)+Chr(10)+Chr(13)+Chr(10)+InitErrorStr
EndIf

Global selectedLanguage$
selectedLanguage = "EN"
Global loadingasset$

Include "Source Code\Language_Core.bb"
Include "Source Code\Keycards_Core.bb"
Include "Source Code\Keys_Core.bb"
Include "Source Code\INI_Core.bb"
Include "Source Code\Math_Core.bb"
Include "Source Code\Strict_Loads_Core.bb"
Include "Source Code\Graphics_Core.bb"
Include "Source Code\Devil_Particle_Core.bb"

Global Font1%, Font2%, Font3%, Font4%, Font5%
Global ConsoleFont%

Const CreatorName$ = "Thaumiel Studios"
Const ModVersion$ = "1.0"
Const VersionNumber$ = "1.3.11"
Global CompatibleNumber$ = "1.3.11" ;Only change this if the version given isn't working with the current build version - ENDSHN

Global MenuWhite%, MenuBlack%
Global ButtonSFX% = LoadSound_Strict("SFX\Interact\Button.ogg")

Global ArrowIMG[4]

Global Depth% = 0

Global WireframeState
Global HalloweenTex

Include "Source Code\AAText_Core.bb"

If LauncherEnabled Then 
	AspectRatioRatio = 1.0
	UpdateLauncher()
	
	;New "fake fullscreen" - ENDSHN Psst, it's called borderless windowed mode --Love Mark,
	If BorderlessWindowed
		DebugLog "Using Borderless Windowed Mode"
		Graphics3DExt DesktopWidth(), DesktopHeight(), 0, 4
		
		RealGraphicWidth = DesktopWidth()
		RealGraphicHeight = DesktopHeight()
		
		AspectRatioRatio = (Float(GraphicWidth)/Float(GraphicHeight))/(Float(RealGraphicWidth)/Float(RealGraphicHeight))
		
		Fullscreen = False
	Else
		AspectRatioRatio = 1.0
		RealGraphicWidth = GraphicWidth
		RealGraphicHeight = GraphicHeight
		If Fullscreen Then
			Graphics3DExt(GraphicWidth, GraphicHeight, 0, 1)
		Else
			Graphics3DExt(GraphicWidth, GraphicHeight, 0, 2)
		EndIf
	EndIf
	
Else
	For i% = 1 To TotalGFXModes
		Local samefound% = False
		For  n% = 0 To TotalGFXModes - 1
			If GfxModeWidths(n) = GfxModeWidth(i) And GfxModeHeights(n) = GfxModeHeight(i) Then samefound = True : Exit
		Next
		If samefound = False Then
			If GraphicWidth = GfxModeWidth(i) And GraphicHeight = GfxModeHeight(i) Then SelectedGFXMode = GFXModes
			GfxModeWidths(GFXModes) = GfxModeWidth(i)
			GfxModeHeights(GFXModes) = GfxModeHeight(i)
			GFXModes=GFXModes+1
		EndIf
	Next
	
	GraphicWidth = GfxModeWidths(SelectedGFXMode)
	GraphicHeight = GfxModeHeights(SelectedGFXMode)
	
	;New "fake fullscreen" - ENDSHN Psst, it's called borderless windowed mode --Love Mark,
	If BorderlessWindowed
		DebugLog "Using Faked Fullscreen"
		Graphics3DExt DesktopWidth(), DesktopHeight(), 0, 4
		
		RealGraphicWidth = DesktopWidth()
		RealGraphicHeight = DesktopHeight()
		
		AspectRatioRatio = (Float(GraphicWidth)/Float(GraphicHeight))/(Float(RealGraphicWidth)/Float(RealGraphicHeight))
		
		Fullscreen = False
	Else
		AspectRatioRatio = 1.0
		RealGraphicWidth = GraphicWidth
		RealGraphicHeight = GraphicHeight
		If Fullscreen Then
			Graphics3DExt(GraphicWidth, GraphicHeight, 0, 1)
		Else
			Graphics3DExt(GraphicWidth, GraphicHeight, 0, 2)
		EndIf
	EndIf
	
EndIf

Global MenuScale# = (GraphicHeight / 1024.0)

SetBuffer(BackBuffer())

Global CurTime%, PrevTime%, LoopDelay%, FPSfactor#, FPSfactor2#, PrevFPSFactor#
Local CheckFPS%, ElapsedLoops%, FPS%, ElapsedTime#

Const HIT_MAP% = 1, HIT_PLAYER% = 2, HIT_ITEM% = 3, HIT_APACHE% = 4, HIT_178% = 5, HIT_DEAD% = 6
SeedRnd MilliSecs()

Global GameSaved%

Global CanSave% = True

AppTitle "SCP - Containment Breach v"+VersionNumber

If PlayStartup Then PlayStartupVideos()

Global CursorIMG% = LoadImage_Strict("GFX\cursor.png")

Global SelectedLoadingScreen.LoadingScreens, LoadingScreenAmount%, LoadingScreenText%
Global LoadingBack% = LoadImage_Strict("Loadingscreens\loadingback.jpg")
InitLoadingScreens("Languages\"+selectedLanguage+"\Loadingscreens.ini")

InitAAFont()
;For some reason, Blitz3D doesn't load fonts that have filenames that
;don't match their "internal name" (i.e. their display name in applications
;like Word and such). As a workaround, I moved the files and renamed them so they
;can load without FastText.
Font1% = AALoadFont("GFX\font\cour\Courier New.ttf", 16)
Font2% = AALoadFont("GFX\font\cour\Courier New.ttf", 52)
Font3% = AALoadFont("GFX\font\DS-DIGI\DS-Digital.ttf", 20)
Font4% = AALoadFont("GFX\font\DS-DIGI\DS-Digital.ttf", 60)
Font5% = AALoadFont("GFX\font\Journal\Journal.ttf", 58)

Global CreditsFont%,CreditsFont2%

ConsoleFont% = AALoadFont("GFX\font\Andale\Andale Mono.ttf", 16)

AASetFont Font2

Global BlinkMeterIMG% = LoadImage_Strict("GFX\blinkmeter.jpg")

loadingasset = "Loading variables"
DrawLoading(0, True)

Global viewport_center_x% = GraphicWidth / 2, viewport_center_y% = GraphicHeight / 2

Global mouselook_x_inc# = 0.3 ; This sets both the sensitivity and direction (+/-) of the mouse on the X axis.
Global mouselook_y_inc# = 0.3 ; This sets both the sensitivity and direction (+/-) of the mouse on the Y axis.
; Used to limit the mouse movement to within a certain number of pixels (250 is used here) from the center of the screen. This produces smoother mouse movement than continuously moving the mouse back to the center each loop.
Global mouse_left_limit% = 250, mouse_right_limit% = GraphicsWidth () - 250
Global mouse_top_limit% = 150, mouse_bottom_limit% = GraphicsHeight () - 150 ; As above.
Global mouse_x_speed_1#, mouse_y_speed_1#

Global KillTimer#, KillAnim%, FallTimer#, DeathTimer#
Global Sanity#, ForceMove#, ForceAngle#
Global RestoreSanity%

Global Playable% = True

Global BLINKFREQ#
Global BlinkTimer#, EyeIrritation#, EyeStuck#, BlinkEffect# = 1.0, BlinkEffectTimer#

Global Stamina#, StaminaEffect#=1.0, StaminaEffectTimer#

Global CameraShakeTimer#, Vomit%, VomitTimer#, Regurgitate%

Global SCP1025state#[6]

Global HeartBeatRate#, HeartBeatTimer#, HeartBeatVolume#

Global WearingGasMask%, WearingHazmat%, WearingVest%, Wearing714%, WearingNightVision%
Global NVTimer#

Global SuperMan%, SuperManTimer#

Global Injuries#, Bloodloss#, Infect#, HealTimer#

Global RefinedItems%

Include "Source Code\Achievements_Core.bb"

Global DropSpeed#, HeadDropSpeed#, CurrSpeed#
Global user_camera_pitch#, side#
Global Crouch%, CrouchState#

Global PlayerZone%

Global GrabbedEntity%

Global MouseHit1%, MouseDown1%, MouseHit2%, DoubleClick%, LastMouseHit1%, MouseUp1%

Global GodMode%, NoClip%, NoClipSpeed# = 2.0

Global CoffinDistance# = 100.0

Global PlayerSoundVolume#

Global Shake#

Global ExplosionTimer#, ExplosionSFX%

Global SoundTransmission%

Global MainMenuOpen%, MenuOpen%, StopHidingTimer#, InvOpen%
Global OtherOpen.Items = Null

Global SelectedEnding%, EndingScreen%, EndingTimer#

Global MsgTimer#, Msg$, DeathMSG$

Global AccessCode%, KeypadInput$, KeypadTimer#, KeypadMSG$

Global DrawHandIcon%
Global DrawArrowIcon%[4]

Include "Source Code\Difficulty_Core.bb"

Global MTFtimer#

Global RadioState#[9]
Global RadioState3%[9]
Global RadioState4%[10]

Global OldAiPics%[2]

Global PlayTime%
Global ConsoleFlush%
Global ConsoleFlushSnd% = 0, ConsoleMusFlush% = 0, ConsoleMusPlay% = 0

Global InfiniteStamina% = False
Global NVBlink%
Global IsNVGBlinking% = False

Global ConsoleOpen%, ConsoleInput$
Global ConsoleScroll#,ConsoleScrollDragging%
Global ConsoleMouseMem%
Global ConsoleReissue.ConsoleMsg = Null
Global ConsoleR% = 255,ConsoleG% = 255,ConsoleB% = 255

Type ConsoleMsg
	Field txt$
	Field isCommand%
	Field r%,g%,b%
End Type

Function CreateConsoleMsg(txt$,r%=-1,g%=-1,b%=-1,isCommand%=False)
	Local c.ConsoleMsg = New ConsoleMsg
	
	Insert c Before First ConsoleMsg
	
	c\txt = txt
	c\isCommand = isCommand
	
	c\r = r
	c\g = g
	c\b = b
	
	If (c\r<0) Then c\r = ConsoleR
	If (c\g<0) Then c\g = ConsoleG
	If (c\b<0) Then c\b = ConsoleB
End Function

Function UpdateConsole()
	Local e.Events
	
	If CanOpenConsole = False Then
		ConsoleOpen = False
		Return
	EndIf
	
	If ConsoleOpen Then
		Local cm.ConsoleMsg
		
		AASetFont ConsoleFont
		
		ConsoleR = 255 : ConsoleG = 255 : ConsoleB = 255
		
		Local x% = 0, y% = GraphicHeight-300*MenuScale, width% = GraphicWidth, height% = 300*MenuScale-30*MenuScale
		Local StrTemp$, temp%,  i%
		Local ev.Events, r.Rooms, it.Items
		
		DrawFrame x,y,width,height+30*MenuScale
		
		Local consoleHeight% = 0
		Local scrollbarHeight% = 0
		
		For cm.ConsoleMsg = Each ConsoleMsg
			consoleHeight = consoleHeight + 15*MenuScale
		Next
		scrollbarHeight = (Float(height)/Float(consoleHeight))*height
		If scrollbarHeight>height Then scrollbarHeight = height
		If consoleHeight<height Then consoleHeight = height
		
		Color 50,50,50
		inBar% = MouseOn(x+width-26*MenuScale,y,26*MenuScale,height)
		If inBar Then Color 70,70,70
		Rect x+width-26*MenuScale,y,26*MenuScale,height,True
		
		Color 120,120,120
		inBox% = MouseOn(x+width-23*MenuScale,y+height-scrollbarHeight+(ConsoleScroll*scrollbarHeight/height),20*MenuScale,scrollbarHeight)
		If inBox Then Color 200,200,200
		If ConsoleScrollDragging Then Color 255,255,255
		Rect x+width-23*MenuScale,y+height-scrollbarHeight+(ConsoleScroll*scrollbarHeight/height),20*MenuScale,scrollbarHeight,True
		
		If Not MouseDown(1) Then
			ConsoleScrollDragging=False
		ElseIf ConsoleScrollDragging Then
			ConsoleScroll = ConsoleScroll+((ScaledMouseY()-ConsoleMouseMem)*height/scrollbarHeight)
			ConsoleMouseMem = ScaledMouseY()
		EndIf
		
		If (Not ConsoleScrollDragging) Then
			If MouseHit1 Then
				If inBox Then
					ConsoleScrollDragging=True
					ConsoleMouseMem = ScaledMouseY()
				ElseIf inBar Then
					ConsoleScroll = ConsoleScroll+((ScaledMouseY()-(y+height))*consoleHeight/height+(height/2))
					ConsoleScroll = ConsoleScroll/2
				EndIf
			EndIf
		EndIf
		
		mouseScroll = MouseZSpeed()
		If mouseScroll=1 Then
			ConsoleScroll = ConsoleScroll - 15*MenuScale
		ElseIf mouseScroll=-1 Then
			ConsoleScroll = ConsoleScroll + 15*MenuScale
		EndIf
		
		Local reissuePos%
		
		If KeyHit(200) Then
			reissuePos% = 0
			If (ConsoleReissue=Null) Then
				ConsoleReissue=First ConsoleMsg
				
				While (ConsoleReissue<>Null)
					If (ConsoleReissue\isCommand) Then
						Exit
					EndIf
					reissuePos = reissuePos - 15*MenuScale
					ConsoleReissue = After ConsoleReissue
				Wend
			Else
				cm.ConsoleMsg = First ConsoleMsg
				While cm<>Null
					If cm=ConsoleReissue Then Exit
					reissuePos = reissuePos-15*MenuScale
					cm = After cm
				Wend
				ConsoleReissue = After ConsoleReissue
				reissuePos = reissuePos-15*MenuScale
				
				While True
					If (ConsoleReissue=Null) Then
						ConsoleReissue=First ConsoleMsg
						reissuePos = 0
					EndIf
				
					If (ConsoleReissue\isCommand) Then
						Exit
					EndIf
					reissuePos = reissuePos - 15*MenuScale
					ConsoleReissue = After ConsoleReissue
				Wend
			EndIf
			If ConsoleReissue<>Null Then
				ConsoleInput = ConsoleReissue\txt
				ConsoleScroll = reissuePos+(height/2)
			EndIf
		EndIf
		
		If KeyHit(208) Then
			reissuePos% = -consoleHeight+15*MenuScale
			If (ConsoleReissue=Null) Then
				ConsoleReissue=Last ConsoleMsg
				
				While (ConsoleReissue<>Null)
					If (ConsoleReissue\isCommand) Then
						Exit
					EndIf
					reissuePos = reissuePos + 15*MenuScale
					ConsoleReissue = Before ConsoleReissue
				Wend
			Else
				cm.ConsoleMsg = Last ConsoleMsg
				While cm<>Null
					If cm=ConsoleReissue Then Exit
					reissuePos = reissuePos+15*MenuScale
					cm = Before cm
				Wend
				ConsoleReissue = Before ConsoleReissue
				reissuePos = reissuePos+15*MenuScale
				
				While True
					If (ConsoleReissue=Null) Then
						ConsoleReissue=Last ConsoleMsg
						reissuePos=-consoleHeight+15*MenuScale
					EndIf
					If (ConsoleReissue\isCommand) Then
						Exit
					EndIf
					reissuePos = reissuePos + 15*MenuScale
					ConsoleReissue = Before ConsoleReissue
				Wend
			EndIf
			If ConsoleReissue<>Null Then
				ConsoleInput = ConsoleReissue\txt
				ConsoleScroll = reissuePos+(height/2)
			EndIf
		EndIf
		
		If ConsoleScroll<-consoleHeight+height Then ConsoleScroll = -consoleHeight+height
		If ConsoleScroll>0 Then ConsoleScroll = 0
		
		Color 255, 255, 255
		
		SelectedInputBox = 2
		
		Local oldConsoleInput$ = ConsoleInput
		
		ConsoleInput = InputBox(x, y + height, width, 30*MenuScale, ConsoleInput, 2)
		If oldConsoleInput<>ConsoleInput Then
			ConsoleReissue = Null
		EndIf
		ConsoleInput = Left(ConsoleInput, 100)
		
		If KeyHit(28) And ConsoleInput <> "" Then
			ConsoleReissue = Null
			ConsoleScroll = 0
			CreateConsoleMsg(ConsoleInput,255,255,0,True)
			If Instr(ConsoleInput, " ") > 0 Then
				StrTemp$ = Lower(Left(ConsoleInput, Instr(ConsoleInput, " ") - 1))
			Else
				StrTemp$ = Lower(ConsoleInput)
			EndIf
			
			Select Lower(StrTemp)
				Case "help"
					;[Block]
					If Instr(ConsoleInput, " ")<>0 Then
						StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Else
						StrTemp$ = ""
					EndIf
					ConsoleR = 0 : ConsoleG = 255 : ConsoleB = 255
					
					Select Lower(StrTemp)
						Case "1",""
							CreateConsoleMsg("LIST OF COMMANDS - PAGE 1/3")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("- asd")
							CreateConsoleMsg("- status")
							CreateConsoleMsg("- camerapick")
							CreateConsoleMsg("- ending")
							CreateConsoleMsg("- noclipspeed")
							CreateConsoleMsg("- noclip")
							CreateConsoleMsg("- injure [value]")
							CreateConsoleMsg("- infect [value]")
							CreateConsoleMsg("- heal")
							CreateConsoleMsg("- teleport [room name]")
							CreateConsoleMsg("- spawnitem [item name]")
							CreateConsoleMsg("- wireframe")
							CreateConsoleMsg("- 173speed")
							CreateConsoleMsg("- 106speed")
							CreateConsoleMsg("- 173state")
							CreateConsoleMsg("- 106state")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Use "+Chr(34)+"help 2/3"+Chr(34)+" to find more commands.")
							CreateConsoleMsg("Use "+Chr(34)+"help [command name]"+Chr(34)+" to get more information about a command.")
							CreateConsoleMsg("******************************")
						Case "2"
							CreateConsoleMsg("LIST OF COMMANDS - PAGE 2/3")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("- spawn [npc type] [state]")
							CreateConsoleMsg("- reset096")
							CreateConsoleMsg("- disable173")
							CreateConsoleMsg("- enable173")
							CreateConsoleMsg("- disable106")
							CreateConsoleMsg("- enable106")
							CreateConsoleMsg("- halloween")
							CreateConsoleMsg("- sanic")
							CreateConsoleMsg("- scp-420-j")
							CreateConsoleMsg("- godmode")
							CreateConsoleMsg("- revive")
							CreateConsoleMsg("- noclip")
							CreateConsoleMsg("- showfps")
							CreateConsoleMsg("- 096state")
							CreateConsoleMsg("- debughud")
							CreateConsoleMsg("- camerafog [near] [far]")
							CreateConsoleMsg("- gamma [value]")
							CreateConsoleMsg("- infinitestamina")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Use "+Chr(34)+"help [command name]"+Chr(34)+" to get more information about a command.")
							CreateConsoleMsg("******************************")
						Case "3"
							CreateConsoleMsg("- playmusic [clip + .wav/.ogg]")
							CreateConsoleMsg("- notarget")
							CreateConsoleMsg("- unlockexits")
						Case "asd"
							CreateConsoleMsg("HELP - asd")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Actives godmode, noclip, wireframe and")
							CreateConsoleMsg("sets fog distance to 20 near, 30 far")
							CreateConsoleMsg("******************************")
						Case "camerafog"
							CreateConsoleMsg("HELP - camerafog")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Sets the draw distance of the fog.")
							CreateConsoleMsg("The fog begins generating at 'CameraFogNear' units")
							CreateConsoleMsg("away from the camera and becomes completely opaque")
							CreateConsoleMsg("at 'CameraFogFar' units away from the camera.")
							CreateConsoleMsg("Example: camerafog 20 40")
							CreateConsoleMsg("******************************")
						Case "gamma"
							CreateConsoleMsg("HELP - gamma")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Sets the gamma correction.")
							CreateConsoleMsg("Should be set to a value between 0.0 and 2.0.")
							CreateConsoleMsg("Default is 1.0.")
							CreateConsoleMsg("******************************")
						Case "noclip","fly"
							CreateConsoleMsg("HELP - noclip")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Toggles noclip, unless a valid parameter")
							CreateConsoleMsg("is specified (on/off).")
							CreateConsoleMsg("Allows the camera to move in any direction while")
							CreateConsoleMsg("bypassing collision.")
							CreateConsoleMsg("******************************")
						Case "godmode","god"
							CreateConsoleMsg("HELP - godmode")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Toggles godmode, unless a valid parameter")
							CreateConsoleMsg("is specified (on/off).")
							CreateConsoleMsg("Prevents player death under normal circumstances.")
							CreateConsoleMsg("******************************")
						Case "wireframe"
							CreateConsoleMsg("HELP - wireframe")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Toggles wireframe, unless a valid parameter")
							CreateConsoleMsg("is specified (on/off).")
							CreateConsoleMsg("Allows only the edges of geometry to be rendered,")
							CreateConsoleMsg("making everything else transparent.")
							CreateConsoleMsg("******************************")
						Case "spawnitem"
							CreateConsoleMsg("HELP - spawnitem")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Spawns an item at the player's location.")
							CreateConsoleMsg("Any name that can appear in your inventory")
							CreateConsoleMsg("is a valid parameter.")
							CreateConsoleMsg("Example: spawnitem Key Card Omni")
							CreateConsoleMsg("******************************")
						Case "spawn"
							CreateConsoleMsg("HELP - spawn")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Spawns an NPC at the player's location.")
							CreateConsoleMsg("Valid parameters are:")
							CreateConsoleMsg("008zombie / 049 / 049-2 / 066 / 096 / 106 / 173")
							CreateConsoleMsg("/ 178-1 / 372 / 513-1 / 966 / 1499-1 / class-d")
							CreateConsoleMsg("/ guard / mtf / apache / tentacle")
							CreateConsoleMsg("******************************")
						Case "revive","undead","resurrect"
							CreateConsoleMsg("HELP - revive")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Resets the player's death timer after the dying")
							CreateConsoleMsg("animation triggers.")
							CreateConsoleMsg("Doesn't affect injury, blood loss")
							CreateConsoleMsg("or 008 infection values.")
							CreateConsoleMsg("******************************")
						Case "teleport"
							CreateConsoleMsg("HELP - teleport")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Teleports the player to the first instance")
							CreateConsoleMsg("of the specified room. Any room that appears")
							CreateConsoleMsg("in rooms.ini is a valid parameter.")
							CreateConsoleMsg("******************************")
						Case "stopsound", "stfu"
							CreateConsoleMsg("HELP - stopsound")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Stops all currently playing sounds.")
							CreateConsoleMsg("******************************")
						Case "camerapick"
							CreateConsoleMsg("HELP - camerapick")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Prints the texture name and coordinates of")
							CreateConsoleMsg("the model the camera is pointing at.")
							CreateConsoleMsg("******************************")
						Case "status"
							CreateConsoleMsg("HELP - status")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Prints player, camera, and room information.")
							CreateConsoleMsg("******************************")
						Case "weed","scp-420-j","420"
							CreateConsoleMsg("HELP - 420")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Generates dank memes.")
							CreateConsoleMsg("******************************")
						Case "playmusic"
							CreateConsoleMsg("HELP - playmusic")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Will play tracks in .ogg/.wav format")
							CreateConsoleMsg("from "+Chr(34)+"SFX\Music\Custom\"+Chr(34)+".")
							CreateConsoleMsg("******************************")
							
						Default
							CreateConsoleMsg("There is no help available for that command.",255,150,0)
					End Select
					
					;[End Block]
				Case "asd"
					;[Block]
					WireFrame 1
					WireframeState=1
					GodMode = 1
					NoClip = 1
					CameraFogNear = 15
					CameraFogFar = 20
					;[End Block]
				Case "status"
					;[Block]
					ConsoleR = 0 : ConsoleG = 255 : ConsoleB = 0
					CreateConsoleMsg("******************************")
					CreateConsoleMsg("Status: ")
					CreateConsoleMsg("Coordinates: ")
					CreateConsoleMsg("    - collider: "+EntityX(Collider)+", "+EntityY(Collider)+", "+EntityZ(Collider))
					CreateConsoleMsg("    - camera: "+EntityX(Camera)+", "+EntityY(Camera)+", "+EntityZ(Camera))
					
					CreateConsoleMsg("Rotation: ")
					CreateConsoleMsg("    - collider: "+EntityPitch(Collider)+", "+EntityYaw(Collider)+", "+EntityRoll(Collider))
					CreateConsoleMsg("    - camera: "+EntityPitch(Camera)+", "+EntityYaw(Camera)+", "+EntityRoll(Camera))
					
					CreateConsoleMsg("Room: "+PlayerRoom\RoomTemplate\Name)
					For ev.Events = Each Events
						If ev\room = PlayerRoom Then
							CreateConsoleMsg("Room event: "+ev\EventName)	
							CreateConsoleMsg("-    state: "+ev\EventState)
							CreateConsoleMsg("-    state2: "+ev\EventState2)	
							CreateConsoleMsg("-    state3: "+ev\EventState3)
							Exit
						EndIf
					Next
					
					CreateConsoleMsg("Room coordinates: "+Floor(EntityX(PlayerRoom\obj) / 8.0 + 0.5)+", "+ Floor(EntityZ(PlayerRoom\obj) / 8.0 + 0.5))
					CreateConsoleMsg("Stamina: "+Stamina)
					CreateConsoleMsg("Death timer: "+KillTimer)					
					CreateConsoleMsg("Blinktimer: "+BlinkTimer)
					CreateConsoleMsg("Injuries: "+Injuries)
					CreateConsoleMsg("Bloodloss: "+Bloodloss)
					CreateConsoleMsg("******************************")
					;[End Block]
				Case "camerapick"
					;[Block]
					ConsoleR = 0 : ConsoleG = 255 : ConsoleB = 0
					c = CameraPick(Camera,GraphicWidth/2, GraphicHeight/2)
					If c = 0 Then
						CreateConsoleMsg("******************************")
						CreateConsoleMsg("No entity  picked")
						CreateConsoleMsg("******************************")								
					Else
						CreateConsoleMsg("******************************")
						CreateConsoleMsg("Picked entity:")
						sf = GetSurface(c,1)
						b = GetSurfaceBrush( sf )
						t = GetBrushTexture(b,0)
						texname$ =  StripPath(TextureName(t))
						CreateConsoleMsg("Texture name: "+texname)
						CreateConsoleMsg("Coordinates: "+EntityX(c)+", "+EntityY(c)+", "+EntityZ(c))
						CreateConsoleMsg("******************************")							
					EndIf
					;[End Block]
				Case "hidedistance"
					;[Block]
					HideDistance = Float(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					CreateConsoleMsg("Hidedistance set to "+HideDistance)
					;[End Block]
				Case "ending"
					;[Block]
					Select StrTemp
						Case "A"
							;[Block]
							SelectedEnding = Rand(Ending_A1, Ending_A2)
							;[End Block]
						Case "B"
							;[Block]
							SelectedEnding = Rand(Ending_B1, Ending_B2)
							;[End Block]
						Default
							;[Block]
							SelectedEnding = Rand(Ending_A1, Ending_B2)
							;[End Block]
					End Select
					
					KillTimer = -0.1
					;[End Block]
				Case "noclipspeed"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					NoClipSpeed = Float(StrTemp)
					;[End Block]
				Case "injure"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Injuries = Float(StrTemp)
					;[End Block]
				Case "infect"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Infect = Float(StrTemp)
					;[End Block]
				Case "heal"
					;[Block]
					Injuries = 0
					Bloodloss = 0
					;[End Block]
				Case "teleport"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "895", "scp-895"
							StrTemp = "coffin"
						Case "scp-914"
							StrTemp = "914"
						Case "offices", "office"
							StrTemp = "room2offices"
					End Select
					
					For r.Rooms = Each Rooms
						If r\RoomTemplate\Name = StrTemp Then
							PositionEntity (Collider, EntityX(r\obj), EntityY(r\obj)+0.7, EntityZ(r\obj))
							ResetEntity(Collider)
							UpdateDoors()
							UpdateRooms()
							For it.Items = Each Items
								it\disttimer = 0
							Next
							PlayerRoom = r
							Exit
						EndIf
					Next
					
					If PlayerRoom\RoomTemplate\Name <> StrTemp Then CreateConsoleMsg("Room not found.",255,150,0)
					;[End Block]
				Case "spawnitem"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					temp = False 
					For itt.Itemtemplates = Each ItemTemplates
						If (Lower(itt\name) = StrTemp) Then
							temp = True
							CreateConsoleMsg(itt\name + " spawned.")
							it.Items = CreateItem(itt\name, itt\tempname, EntityX(Collider), EntityY(Camera,True), EntityZ(Collider))
							EntityType(it\collider, HIT_ITEM)
							Exit
						ElseIf (Lower(itt\tempname) = StrTemp) Then
							temp = True
							CreateConsoleMsg(itt\name + " spawned.")
							it.Items = CreateItem(itt\name, itt\tempname, EntityX(Collider), EntityY(Camera,True), EntityZ(Collider))
							EntityType(it\collider, HIT_ITEM)
							Exit
						EndIf
					Next
					
					If temp = False Then CreateConsoleMsg("Item not found.",255,150,0)
					;[End Block]
				Case "wireframe"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							WireframeState = True							
						Case "off", "0", "false"
							WireframeState = False
						Default
							WireframeState = Not WireframeState
					End Select
					
					If WireframeState Then
						CreateConsoleMsg("WIREFRAME ON")
					Else
						CreateConsoleMsg("WIREFRAME OFF")	
					EndIf
					
					WireFrame WireframeState
					;[End Block]
				Case "173speed"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Curr173\Speed = Float(StrTemp)
					CreateConsoleMsg("173's speed set to " + StrTemp)
					;[End Block]
				Case "106speed"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Curr106\Speed = Float(StrTemp)
					CreateConsoleMsg("106's speed set to " + StrTemp)
					;[End Block]
				Case "173state"
					;[Block]
					CreateConsoleMsg("SCP-173")
					CreateConsoleMsg("Position: " + EntityX(Curr173\obj) + ", " + EntityY(Curr173\obj) + ", " + EntityZ(Curr173\obj))
					CreateConsoleMsg("Idle: " + Curr173\Idle)
					CreateConsoleMsg("State: " + Curr173\State)
					;[End Block]
				Case "106state"
					;[Block]
					CreateConsoleMsg("SCP-106")
					CreateConsoleMsg("Position: " + EntityX(Curr106\obj) + ", " + EntityY(Curr106\obj) + ", " + EntityZ(Curr106\obj))
					CreateConsoleMsg("Idle: " + Curr106\Idle)
					CreateConsoleMsg("State: " + Curr106\State)
					;[End Block]
				Case "reset096"
					;[Block]
					For n.NPCs = Each NPCs
						If n\NPCtype = NPCtype096 Then
							n\State = 0
							StopStream_Strict(n\SoundChn) : n\SoundChn=0
							If n\SoundChn2<>0
								StopStream_Strict(n\SoundChn2) : n\SoundChn2=0
							EndIf
							Exit
						EndIf
					Next
					;[End Block]
				Case "disable173"
					;[Block]
					Curr173\Idle = 3 ;This phenominal comment is brought to you by PolyFox. His absolute wisdom in this fatigue of knowledge brought about a new era of 173 state checks.
					HideEntity Curr173\obj
					HideEntity Curr173\Collider
					;[End Block]
				Case "enable173"
					;[Block]
					Curr173\Idle = False
					ShowEntity Curr173\obj
					ShowEntity Curr173\Collider
					;[End Block]
				Case "disable106"
					;[Block]
					Curr106\Idle = True
					Curr106\State = 200000
					Contained106 = True
					;[End Block]
				Case "enable106"
					;[Block]
					Curr106\Idle = False
					Contained106 = False
					ShowEntity Curr106\Collider
					ShowEntity Curr106\obj
					;[End Block]
				Case "halloween"
					;[Block]
					HalloweenTex = Not HalloweenTex
					If HalloweenTex Then
						Local tex = LoadTexture_Strict("GFX\npcs\173h.pt", 1)
						EntityTexture Curr173\obj, tex, 0, 0
						FreeTexture tex
						CreateConsoleMsg("173 JACK-O-LANTERN ON")
					Else
						Local tex2 = LoadTexture_Strict("GFX\npcs\173texture.jpg", 1)
						EntityTexture Curr173\obj, tex2, 0, 0
						FreeTexture tex2
						CreateConsoleMsg("173 JACK-O-LANTERN OFF")
					EndIf
					;[End Block]
				Case "sanic"
					;[Block]
					SuperMan = Not SuperMan
					If SuperMan = True Then
						CreateConsoleMsg("GOTTA GO FAST")
					Else
						CreateConsoleMsg("WHOA SLOW DOWN")
					EndIf
					;[End Block]
				Case "scp-420-j","420","weed"
					;[Block]
					For i = 1 To 20
						If Rand(2)=1 Then
							it.Items = CreateItem("Some SCP-420-J","420", EntityX(Collider,True)+Cos((360.0/20.0)*i)*Rnd(0.3,0.5), EntityY(Camera,True), EntityZ(Collider,True)+Sin((360.0/20.0)*i)*Rnd(0.3,0.5))
						Else
							it.Items = CreateItem("Joint","420s", EntityX(Collider,True)+Cos((360.0/20.0)*i)*Rnd(0.3,0.5), EntityY(Camera,True), EntityZ(Collider,True)+Sin((360.0/20.0)*i)*Rnd(0.3,0.5))
						EndIf
						EntityType (it\collider, HIT_ITEM)
					Next
					PlaySound_Strict LoadTempSound("SFX\Music\420J.ogg")
					;[End Block]
				Case "godmode", "god"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							GodMode = True						
						Case "off", "0", "false"
							GodMode = False
						Default
							GodMode = Not GodMode
					End Select	
					If GodMode Then
						CreateConsoleMsg("GODMODE ON")
					Else
						CreateConsoleMsg("GODMODE OFF")	
					EndIf
					;[End Block]
				Case "revive","undead","resurrect"
					;[Block]
					DropSpeed = -0.1
					HeadDropSpeed = 0.0
					Shake = 0
					CurrSpeed = 0
					
					HeartBeatVolume = 0
					
					CameraShake = 0
					Shake = 0
					LightFlash = 0
					BlurTimer = 0
					
					FallTimer = 0
					MenuOpen = False
					
					GodMode = 0
					NoClip = 0
					
					ShowEntity Collider
					
					KillTimer = 0
					KillAnim = 0
					;[End Block]
				Case "noclip","fly"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							NoClip = True
							Playable = True
						Case "off", "0", "false"
							NoClip = False	
							RotateEntity Collider, 0, EntityYaw(Collider), 0
						Default
							NoClip = Not NoClip
							If NoClip = False Then		
								RotateEntity Collider, 0, EntityYaw(Collider), 0
							Else
								Playable = True
							EndIf
					End Select
					
					If NoClip Then
						CreateConsoleMsg("NOCLIP ON")
					Else
						CreateConsoleMsg("NOCLIP OFF")
					EndIf
					
					DropSpeed = 0
					;[End Block]
				Case "showfps"
					;[Block]
					ShowFPS = Not ShowFPS
					CreateConsoleMsg("ShowFPS: "+Str(ShowFPS))
					;[End Block]
				Case "096state"
					;[Block]
					For n.NPCs = Each NPCs
						If n\NPCtype = NPCtype096 Then
							CreateConsoleMsg("SCP-096")
							CreateConsoleMsg("Position: " + EntityX(n\obj) + ", " + EntityY(n\obj) + ", " + EntityZ(n\obj))
							CreateConsoleMsg("Idle: " + n\Idle)
							CreateConsoleMsg("State: " + n\State)
							Exit
						EndIf
					Next
					CreateConsoleMsg("SCP-096 has not spawned.")
					;[End Block]
				Case "debughud"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Select StrTemp
						Case "on", "1", "true"
							DebugHUD = True
						Case "off", "0", "false"
							DebugHUD = False
						Default
							DebugHUD = Not DebugHUD
					End Select
					
					If DebugHUD Then
						CreateConsoleMsg("Debug Mode On")
					Else
						CreateConsoleMsg("Debug Mode Off")
					EndIf
					;[End Block]
				Case "stopsound", "stfu"
					;[Block]
					For snd.Sound = Each Sound
						For i = 0 To 31
							If snd\channels[i]<>0 Then
								StopChannel snd\channels[i]
							EndIf
						Next
					Next
					
					For e.Events = Each Events
						If e\EventName = "alarm" Then 
							If e\room\NPC[0] <> Null Then RemoveNPC(e\room\NPC[0])
							If e\room\NPC[1] <> Null Then RemoveNPC(e\room\NPC[1])
							If e\room\NPC[2] <> Null Then RemoveNPC(e\room\NPC[2])
							
							FreeEntity e\room\Objects[0] : e\room\Objects[0]=0
							FreeEntity e\room\Objects[1] : e\room\Objects[1]=0
							PositionEntity Curr173\Collider, 0,0,0
							ResetEntity Curr173\Collider
							ShowEntity Curr173\obj
							RemoveEvent(e)
							Exit
						EndIf
					Next
					CreateConsoleMsg("Stopped all sounds.")
					;[End Block]
				Case "camerafog"
					;[Block]
					args$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					CameraFogNear = Float(Left(args, Len(args) - Instr(args, " ")))
					CameraFogFar = Float(Right(args, Len(args) - Instr(args, " ")))
					CreateConsoleMsg("Near set to: " + CameraFogNear + ", far set to: " + CameraFogFar)
					;[End Block]
				Case "gamma"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					ScreenGamma = Int(StrTemp)
					CreateConsoleMsg("Gamma set to " + ScreenGamma)
					;[End Block]
				Case "spawn"
					;[Block]
					args$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					StrTemp$ = Piece$(args$, 1)
					StrTemp2$ = Piece$(args$, 2)
					
					;Hacky fix for when the user doesn't input a second parameter.
					If (StrTemp <> StrTemp2) Then
						Console_SpawnNPC(StrTemp, StrTemp2)
					Else
						Console_SpawnNPC(StrTemp)
					EndIf
					;[End Block]
				Case "infinitestamina","infstam"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							InfiniteStamina% = True						
						Case "off", "0", "false"
							InfiniteStamina% = False
						Default
							InfiniteStamina% = Not InfiniteStamina%
					End Select
					
					If InfiniteStamina
						CreateConsoleMsg("INFINITE STAMINA ON")
					Else
						CreateConsoleMsg("INFINITE STAMINA OFF")	
					EndIf
					;[End Block]
				Case "asd2"
					;[Block]
					GodMode = 1
					InfiniteStamina = 1
					Curr173\Idle = 3
					Curr106\Idle = True
					Curr106\State = 200000
					Contained106 = True
					;[End Block]
				Case "toggle_warhead_lever"
					;[Block]
					For e.Events = Each Events
						If e\EventName = "room2nuke" Then
							e\EventState = (Not e\EventState)
							Exit
						EndIf
					Next
					;[End Block]
				Case "unlockexits"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "a"
							For e.Events = Each Events
								If e\EventName = "gateaentrance" Then
									e\EventState3 = 1
									e\room\RoomDoors[1]\open = True
									Exit
								EndIf
							Next
							CreateConsoleMsg("Gate A is now unlocked.")	
						Case "b"
							For e.Events = Each Events
								If e\EventName = "exit1" Then
									e\EventState3 = 1
									e\room\RoomDoors[4]\open = True
									Exit
								EndIf
							Next	
							CreateConsoleMsg("Gate B is now unlocked.")	
						Default
							For e.Events = Each Events
								If e\EventName = "gateaentrance" Then
									e\EventState3 = 1
									e\room\RoomDoors[1]\open = True
								ElseIf e\EventName = "exit1" Then
									e\EventState3 = 1
									e\room\RoomDoors[4]\open = True
								EndIf
							Next
							CreateConsoleMsg("Gate A and B are now unlocked.")	
					End Select
					
					RemoteDoorOn = True
					;[End Block]
				Case "kill","suicide"
					;[Block]
					KillTimer = -1
					Select Rand(4)
						Case 1
							DeathMSG = "[REDACTED]"
						Case 2
							DeathMSG = "Subject D-9341 found dead in Sector [REDACTED]. "
							DeathMSG = DeathMSG + "The subject appears to have attained no physical damage, and there is no visible indication as to what killed him. "
							DeathMSG = DeathMSG + "Body was sent for autopsy."
						Case 3
							DeathMSG = "EXCP_ACCESS_VIOLATION"
						Case 4
							DeathMSG = "Subject D-9341 found dead in Sector [REDACTED]. "
							DeathMSG = DeathMSG + "The subject appears to have scribbled the letters "+Chr(34)+"kys"+Chr(34)+" in his own blood beside him. "
							DeathMSG = DeathMSG + "No other signs of physical trauma or struggle can be observed. Body was sent for autopsy."
					End Select
					;[End Block]
				Case "playmusic"
					;[Block]
					; I think this might be broken since the FMod library streaming was added. -Mark
					If Instr(ConsoleInput, " ")<>0 Then
						StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Else
						StrTemp$ = ""
					EndIf
					
					If StrTemp$ <> ""
						PlayCustomMusic% = True
						If CustomMusic <> 0 Then FreeSound_Strict CustomMusic : CustomMusic = 0
						If MusicCHN <> 0 Then StopChannel MusicCHN
						CustomMusic = LoadSound_Strict("SFX\Music\Custom\"+StrTemp$)
						If CustomMusic = 0
							PlayCustomMusic% = False
						EndIf
					Else
						PlayCustomMusic% = False
						If CustomMusic <> 0 Then FreeSound_Strict CustomMusic : CustomMusic = 0
						If MusicCHN <> 0 Then StopChannel MusicCHN
					EndIf
					;[End Block]
				Case "tp"
					;[Block]
					For n.NPCs = Each NPCs
						If n\NPCtype = NPCtypeMTF
							If n\MTFLeader = Null
								PositionEntity Collider,EntityX(n\Collider),EntityY(n\Collider)+5,EntityZ(n\Collider)
								ResetEntity Collider
								Exit
							EndIf
						EndIf
					Next
					;[End Block]
				Case "tele"
					;[Block]
					args$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					StrTemp$ = Piece$(args$,1," ")
					StrTemp2$ = Piece$(args$,2," ")
					StrTemp3$ = Piece$(args$,3," ")
					PositionEntity Collider,Float(StrTemp$),Float(StrTemp2$),Float(StrTemp3$)
					PositionEntity Camera,Float(StrTemp$),Float(StrTemp2$),Float(StrTemp3$)
					ResetEntity Collider
					ResetEntity Camera
					CreateConsoleMsg("Teleported to coordinates (X|Y|Z): "+EntityX(Collider)+"|"+EntityY(Collider)+"|"+EntityZ(Collider))
					;[End Block]
				Case "notarget"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							NoTarget% = True						
						Case "off", "0", "false"
							NoTarget% = False	
						Default
							NoTarget% = Not NoTarget%
					End Select
					
					If NoTarget% = False Then
						CreateConsoleMsg("NOTARGET OFF")
					Else
						CreateConsoleMsg("NOTARGET ON")	
					EndIf
					;[End Block]
				Case "spawnradio"
					;[Block]
					it.Items = CreateItem("Radio Transceiver", "fineradio", EntityX(Collider), EntityY(Camera,True), EntityZ(Collider))
					EntityType(it\collider, HIT_ITEM)
					it\state = 101
					;[End Block]
				Case "spawnnvg"
					;[Block]
					it.Items = CreateItem("Night Vision Goggles", "nvgoggles", EntityX(Collider), EntityY(Camera,True), EntityZ(Collider))
					EntityType(it\collider, HIT_ITEM)
					it\state = 1000
					;[End Block]
				Case "spawnpumpkin","pumpkin"
					;[Block]
					CreateConsoleMsg("What pumpkin?")
					;[End Block]
				Case "spawnnav"
					;[Block]
					it.Items = CreateItem("S-NAV Navigator Ultimate", "nav", EntityX(Collider), EntityY(Camera,True), EntityZ(Collider))
					EntityType(it\collider, HIT_ITEM)
					it\state = 101
					;[End Block]
				Case "teleport173"
					;[Block]
					PositionEntity Curr173\Collider,EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider)
					ResetEntity Curr173\Collider
					;[End Block]
				Case "seteventstate"
					;[Block]
					args$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					StrTemp$ = Piece$(args$,1," ")
					StrTemp2$ = Piece$(args$,2," ")
					StrTemp3$ = Piece$(args$,3," ")
					Local pl_room_found% = False
					If StrTemp="" Lor StrTemp2="" Lor StrTemp3=""
						CreateConsoleMsg("Too few parameters. This command requires 3.",255,150,0)
					Else
						For e.Events = Each Events
							If e\room = PlayerRoom
								If Lower(StrTemp)<>"keep"
									e\EventState = Float(StrTemp)
								EndIf
								If Lower(StrTemp2)<>"keep"
									e\EventState2 = Float(StrTemp2)
								EndIf
								If Lower(StrTemp3)<>"keep"
									e\EventState3 = Float(StrTemp3)
								EndIf
								CreateConsoleMsg("Changed event states from current player room to: "+e\EventState+"|"+e\EventState2+"|"+e\EventState3)
								pl_room_found = True
								Exit
							EndIf
						Next
						If (Not pl_room_found)
							CreateConsoleMsg("The current room doesn't has any event applied.",255,150,0)
						EndIf
					EndIf
					;[End Block]
				Case "spawnparticles"
					;[Block]
					If Instr(ConsoleInput, " ")<>0 Then
						StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Else
						StrTemp$ = ""
					EndIf
					
					If Int(StrTemp) > -1 And Int(StrTemp) <= 1 ;<--- This is the maximum ID of particles by Devil Particle system, will be increased after time - ENDSHN
						SetEmitter(Collider,ParticleEffect[Int(StrTemp)])
						CreateConsoleMsg("Spawned particle emitter with ID "+Int(StrTemp)+" at player's position.")
					Else
						CreateConsoleMsg("Particle emitter with ID "+Int(StrTemp)+" not found.",255,150,0)
					EndIf
					;[End Block]
				Case "giveachievement"
					;[Block]
					If Instr(ConsoleInput, " ")<>0 Then
						StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Else
						StrTemp$ = ""
					EndIf
					
					If Int(StrTemp)>=0 And Int(StrTemp)<MAXACHIEVEMENTS
						Achievements[Int(StrTemp)]=True
						CreateConsoleMsg("Achievemt "+AchievementStrings[Int(StrTemp)]+" unlocked.")
					Else
						CreateConsoleMsg("Achievement with ID "+Int(StrTemp)+" doesn't exist.",255,150,0)
					EndIf
					;[End Block]
				Case "427state"
					;[Block]
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					I_427\Timer = Float(StrTemp)*70.0
					;[End Block]
				Case "teleport106"
					;[Block]
					Curr106\State = 0
					Curr106\Idle = False
					;[End Block]
				Case "setblinkeffect"
					;[Block]
					args$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					BlinkEffect = Float(Left(args, Len(args) - Instr(args, " ")))
					BlinkEffectTimer = Float(Right(args, Len(args) - Instr(args, " ")))
					CreateConsoleMsg("Set BlinkEffect to: " + BlinkEffect + "and BlinkEffect timer: " + BlinkEffectTimer)
					;[End Block]
				Case "jorge"
					;[Block]	
					CreateConsoleMsg(Chr(74)+Chr(79)+Chr(82)+Chr(71)+Chr(69)+Chr(32)+Chr(72)+Chr(65)+Chr(83)+Chr(32)+Chr(66)+Chr(69)+Chr(69)+Chr(78)+Chr(32)+Chr(69)+Chr(88)+Chr(80)+Chr(69)+Chr(67)+Chr(84)+Chr(73)+Chr(78)+Chr(71)+Chr(32)+Chr(89)+Chr(79)+Chr(85)+Chr(46))
					;[End Block]
				Default
					;[Block]
					CreateConsoleMsg("Command not found.",255,0,0)
					;[End Block]
			End Select
			
			ConsoleInput = ""
		EndIf
		
		Local TempY% = y + height - 25*MenuScale - ConsoleScroll
		Local count% = 0
		
		For cm.ConsoleMsg = Each ConsoleMsg
			count = count+1
			If count>1000 Then
				Delete cm
			Else
				If TempY >= y And TempY < y + height - 20*MenuScale Then
					If cm=ConsoleReissue Then
						Color cm\r/4,cm\g/4,cm\b/4
						Rect x,TempY-2*MenuScale,width-30*MenuScale,24*MenuScale,True
					EndIf
					Color cm\r,cm\g,cm\b
					If cm\isCommand Then
						AAText(x + 20*MenuScale, TempY, "> "+cm\txt)
					Else
						AAText(x + 20*MenuScale, TempY, cm\txt)
					EndIf
				EndIf
				TempY = TempY - 15*MenuScale
			EndIf
		Next
		
		Color 255,255,255
		
		If Fullscreen Then DrawImage CursorIMG, ScaledMouseX(),ScaledMouseY()
	EndIf
	
	AASetFont Font1
End Function

ConsoleR = 0 : ConsoleG = 255 : ConsoleB = 255

CreateConsoleMsg("Console commands: ")
CreateConsoleMsg("  - teleport [room name]")
CreateConsoleMsg("  - godmode [on/off]")
CreateConsoleMsg("  - noclip [on/off]")
CreateConsoleMsg("  - noclipspeed [x] (default = 2.0)")
CreateConsoleMsg("  - wireframe [on/off]")
CreateConsoleMsg("  - debughud [on/off]")
CreateConsoleMsg("  - camerafog [near] [far]")
CreateConsoleMsg(" ")
CreateConsoleMsg("  - status")
CreateConsoleMsg("  - heal")
CreateConsoleMsg(" ")
CreateConsoleMsg("  - spawnitem [item name]")
CreateConsoleMsg(" ")
CreateConsoleMsg("  - 173speed [x] (default = 35)")
CreateConsoleMsg("  - disable173/enable173")
CreateConsoleMsg("  - disable106/enable106")
CreateConsoleMsg("  - 173state/106state/096state")
CreateConsoleMsg("  - spawn [npc type]")

Global DebugHUD%

Global BlurVolume#, BlurTimer#

Global LightBlink#, LightFlash#

Global Camera%, CameraShake#, CurrCameraZoom#

Global StoredCameraFogFar# = CameraFogFar

Include "Source Code\Sounds_Core.bb"

;This variable is for when a camera detected the player
	;False: Player is not seen (will be set after every call of the Main Loop
	;True: The Player got detected by a camera
Global PlayerDetected%
Global PrevInjuries#,PrevBloodloss#
Global NoTarget% = False

Global NVGImages = LoadAnimImage("GFX\battery.png",64,64,0,2)
MaskImage NVGImages,255,0,255

Global Wearing1499% = False

Global NTF_1499PrevX#
Global NTF_1499PrevY#
Global NTF_1499PrevZ#
Global NTF_1499PrevRoom.Rooms
Global NTF_1499X#
Global NTF_1499Y#
Global NTF_1499Z#
Global NTF_1499Sky%

Global OptionsMenu% = 0
Global QuitMSG% = 0

Global InFacility% = True

Global DeafPlayer% = False
Global DeafTimer# = 0.0

Global IsZombie% = False

Global NavImages[5]
For i = 0 To 3
	NavImages[i] = LoadImage_Strict("GFX\navigator\roomborder"+i+".png")
	MaskImage NavImages[i],255,0,255
Next
NavImages[4] = LoadImage_Strict("GFX\navigator\batterymeter.png")

Global NavBG = CreateImage(GraphicWidth,GraphicHeight)

Global ParticleEffect[10]

Const MaxDTextures=8
Global DTextures[MaxDTextures]

Global NPC049OBJ, NPC0492OBJ
Global ClerkOBJ

Global ForestNPC,ForestNPCTex,ForestNPCData#[3]

Global PauseMenuIMG%

Global SprintIcon%
Global BlinkIcon%
Global CrouchIcon%
Global HandIcon%
Global HandIcon2%

Global StaminaMeterIMG%

Global KeypadHUD

Global Panel294, Using294%, Input294$

loadingasset = "Loading items and particles"
DrawLoading(35, True)

Include "Source Code\Items_Core.bb"

Include "Source Code\Particles_Core.bb"

loadingasset = "Loading map"
DrawLoading(40,True)

Include "Source Code\Map_Core.bb"

loadingasset = "Loading NPCs and events"
DrawLoading(80,True)

Include "Source Code\NPCs_Core.bb"

Include "Source Code\Events_Core.bb"

Collisions HIT_PLAYER, HIT_MAP, 2, 2
Collisions HIT_PLAYER, HIT_PLAYER, 1, 3
Collisions HIT_ITEM, HIT_MAP, 2, 2
Collisions HIT_APACHE, HIT_APACHE, 1, 2
Collisions HIT_178, HIT_MAP, 2, 2
Collisions HIT_178, HIT_178, 1, 3
Collisions HIT_DEAD, HIT_MAP, 2, 2

loadingasset = "Loading variables"
DrawLoading(90, True)

Global FogTexture%, Fog%
Global GasMaskTexture%, GasMaskOverlay%
Global InfectTexture%, InfectOverlay%
Global DarkTexture%, Dark%
Global Collider%, Head%

Global FogNVTexture%
Global NVTexture%, NVOverlay%

Global TeslaTexture%

Global LightTexture%, Light%
Global DoorOBJ%, DoorFrameOBJ%

Global LeverOBJ%, LeverBaseOBJ%

Global DoorColl%
Global ButtonOBJ%, ButtonKeyOBJ%, ButtonCodeOBJ%, ButtonScannerOBJ%

Global Monitor%, MonitorTexture%
Global CamBaseOBJ%, CamOBJ%

Global LiquidObj%,MTFObj%,GuardObj%,ClassDObj%
Global ApacheObj%,ApacheRotorObj%

Global UnableToMove% = False
Global ShouldEntitiesFall% = True
Global PlayerFallingPickDistance# = 10.0

Global MTF_CameraCheckTimer# = 0.0
Global MTF_CameraCheckDetected% = False

Include "Source Code\Menu_Core.bb"
MainMenuOpen = True

FlushKeys()
FlushMouse()

DrawLoading(100, True)

LoopDelay = MilliSecs()

Global UpdateParticles_Time# = 0.0

Global CurrTrisAmount%

Global Input_ResetTime# = 0

Type SCP427
	Field Using%
	Field Timer#
	Field Sound[2]
	Field SoundCHN[2]
End Type

Global I_427.SCP427 = New SCP427

Type MapZones
	Field Transition%[2]
	Field HasCustomForest%
	Field HasCustomMT%
End Type

Global I_Zone.MapZones = New MapZones

Function CatchErrors(Location$)
	InitErrorMsgs(6)
	SetErrorMsg(0, "An error occured in SCP - Containment Breach v" + VersionNumber)
	SetErrorMsg(1, "Map Seed: " + RandomSeed)
	SetErrorMsg(2, "Date and time: " + CurrentDate() + " at " + CurrentTime() + Chr(10) + "OS: " + SystemProperty("os") + " " + (32 + (GetEnv("ProgramFiles(X86)") <> 0) * 32) + " bit (Build: " + SystemProperty("osbuild") + ")" + Chr(10))
	SetErrorMsg(3, "Video memory: " + ((TotalVidMem() / 1024) - (AvailVidMem() / 1024)) + " MB/" + (TotalVidMem() / 1024) + " MB" + Chr(10))
	SetErrorMsg(4, "Global memory status: " + ((TotalPhys() / 1024) - (AvailPhys() / 1024)) + " MB/" + (TotalPhys() / 1024) + " MB" + Chr(10))
	SetErrorMsg(5, "Error located in: " + Location + Chr(10) + Chr(10) + "Please take a screenshot of this error and send it to us!") 
End Function

;----------------------------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------       		MAIN LOOP                 ---------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------

Repeat
	Cls
	
	CurTime = MilliSecs2()
	ElapsedTime = (CurTime - PrevTime) / 1000.0
	PrevTime = CurTime
	PrevFPSFactor = FPSfactor
	FPSfactor = Max(Min(ElapsedTime * 70, 5.0), 0.2)
	FPSfactor2 = FPSfactor
	
	If MenuOpen Lor InvOpen Lor OtherOpen<>Null Lor ConsoleOpen Lor SelectedDoor <> Null Lor SelectedScreen <> Null Lor Using294 Then FPSfactor = 0
	
	If Framelimit > 0 Then
		;Framelimit
		Local WaitingTime% = (1000.0 / Framelimit) - (MilliSecs2() - LoopDelay)
		
		Delay WaitingTime%
		
		LoopDelay = MilliSecs2()
	EndIf
	
	;Counting the fps
	If CheckFPS < MilliSecs2() Then
		FPS = ElapsedLoops
		ElapsedLoops = 0
		CheckFPS = MilliSecs2()+1000
	EndIf
	ElapsedLoops = ElapsedLoops + 1
	
	If Input_ResetTime<=0.0
		DoubleClick = False
		MouseHit1 = MouseHit(1)
		If MouseHit1 Then
			If MilliSecs2() - LastMouseHit1 < 800 Then DoubleClick = True
			LastMouseHit1 = MilliSecs2()
		EndIf
		
		Local prevmousedown1 = MouseDown1
		MouseDown1 = MouseDown(1)
		If prevmousedown1 = True And MouseDown1=False Then MouseUp1 = True Else MouseUp1 = False
		
		MouseHit2 = MouseHit(2)
		
		If (Not MouseDown1) And (Not MouseHit1) Then GrabbedEntity = 0
	Else
		Input_ResetTime = Max(Input_ResetTime-FPSfactor,0.0)
	EndIf
	
	UpdateMusic()
	If EnableSFXRelease Then AutoReleaseSounds()
	
	If MainMenuOpen Then
		If ShouldPlay = 21 Then
			EndBreathSFX = LoadSound("SFX\Ending\MenuBreath.ogg")
			EndBreathCHN = PlaySound(EndBreathSFX)
			ShouldPlay = 66
		ElseIf ShouldPlay = 66
			If (Not ChannelPlaying(EndBreathCHN)) Then
				FreeSound(EndBreathSFX)
				ShouldPlay = 11
			EndIf
		Else
			ShouldPlay = 11
		EndIf
		UpdateMainMenu()
	Else
		UpdateStreamSounds()
		
		ShouldPlay = Min(PlayerZone,2)
		
		DrawHandIcon = False
		
		RestoreSanity = True
		ShouldEntitiesFall = True
		
		If FPSfactor > 0 And PlayerRoom\RoomTemplate\Name <> "dimension1499" Then UpdateSecurityCams()
		
		If PlayerRoom\RoomTemplate\Name <> "pocketdimension" And PlayerRoom\RoomTemplate\Name <> "gatea" And PlayerRoom\RoomTemplate\Name <> "exit1" And (Not MenuOpen) And (Not ConsoleOpen) And (Not InvOpen) Then 
			If Rand(1500) = 1 Then
				For i = 0 To 5
					If AmbientSFX(i,CurrAmbientSFX)<>0 Then
						If ChannelPlaying(AmbientSFXCHN)=0 Then FreeSound_Strict AmbientSFX(i,CurrAmbientSFX) : AmbientSFX(i,CurrAmbientSFX) = 0
					EndIf			
				Next
				
				PositionEntity (SoundEmitter, EntityX(Camera) + Rnd(-1.0, 1.0), 0.0, EntityZ(Camera) + Rnd(-1.0, 1.0))
				
				If Rand(3)=1 Then PlayerZone = 3
				
				If PlayerRoom\RoomTemplate\Name = "173" Then 
					PlayerZone = 4
				ElseIf PlayerRoom\RoomTemplate\Name = "room860"
					For e.Events = Each Events
						If e\EventName = "room860"
							If e\EventState = 1.0
								PlayerZone = 5
								PositionEntity (SoundEmitter, EntityX(SoundEmitter), 30.0, EntityZ(SoundEmitter))
							EndIf
							
							Exit
						EndIf
					Next
				EndIf
				
				CurrAmbientSFX = Rand(0,AmbientSFXAmount[PlayerZone]-1)
				
				Select PlayerZone
					Case 0,1,2
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\Ambient\Zone"+(PlayerZone+1)+"\ambient"+(CurrAmbientSFX+1)+".ogg")
					Case 3
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\Ambient\General\ambient"+(CurrAmbientSFX+1)+".ogg")
					Case 4
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\Ambient\Pre-breach\ambient"+(CurrAmbientSFX+1)+".ogg")
					Case 5
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\Ambient\Forest\ambient"+(CurrAmbientSFX+1)+".ogg")
				End Select
				
				AmbientSFXCHN = PlaySound2(AmbientSFX(PlayerZone,CurrAmbientSFX), Camera, SoundEmitter)
			EndIf
			UpdateSoundOrigin(AmbientSFXCHN,Camera, SoundEmitter)
			
			If Rand(50000) = 3 Then
				Local RN$ = PlayerRoom\RoomTemplate\Name$
				
				If RN$ <> "room860" And RN$ <> "room1123" And RN$ <> "173" And RN$ <> "dimension1499" Then
					If FPSfactor > 0 Then LightBlink = Rnd(1.0,2.0)
					PlaySound_Strict  LoadTempSound("SFX\SCP\079\Broadcast"+Rand(1,7)+".ogg")
				EndIf 
			EndIf
		EndIf
		
		UpdateCheckpoint1 = False
		UpdateCheckpoint2 = False
		
		If (Not MenuOpen) And (Not InvOpen) And (OtherOpen=Null) And (SelectedDoor = Null) And (ConsoleOpen = False) And (Using294 = False) And (SelectedScreen = Null) And EndingTimer=>0 Then
			LightVolume = CurveValue(TempLightVolume, LightVolume, 50.0)
			CameraFogRange(Camera, CameraFogNear*LightVolume,CameraFogFar*LightVolume)
			CameraFogColor(Camera, 0,0,0)
			CameraFogMode Camera,1
			CameraRange(Camera, 0.05, Min(CameraFogFar*LightVolume*1.5,28))	
			If PlayerRoom\RoomTemplate\Name<>"pocketdimension" Then
				CameraClsColor(Camera, 0,0,0)
			EndIf
			
			AmbientLight BRIGHTNESS, BRIGHTNESS, BRIGHTNESS	
			PlayerSoundVolume = CurveValue(0.0, PlayerSoundVolume, 5.0)
			
			CanSave% = True
			UpdateDeafPlayer()
			UpdateEmitters()
			MouseLook()
			If PlayerRoom\RoomTemplate\Name = "dimension1499" And QuickLoadPercent > 0 And QuickLoadPercent < 100
				ShouldEntitiesFall = False
			EndIf
			MovePlayer()
			InFacility = CheckForPlayerInFacility()
			If PlayerRoom\RoomTemplate\Name = "dimension1499"
				If QuickLoadPercent = -1 Lor QuickLoadPercent = 100
					UpdateDimension1499()
				EndIf
				UpdateLeave1499()
			ElseIf PlayerRoom\RoomTemplate\Name = "gatea" Lor (PlayerRoom\RoomTemplate\Name="exit1" And EntityY(Collider)>1040.0*RoomScale)
				UpdateDoors()
				If QuickLoadPercent = -1 Lor QuickLoadPercent = 100
					UpdateEndings()
				EndIf
				UpdateScreens()
				UpdateRoomLights(Camera)
			Else
				UpdateDoors()
				If QuickLoadPercent = -1 Lor QuickLoadPercent = 100
					UpdateEvents()
				EndIf
				UpdateScreens()
				TimeCheckpointMonitors()
				Update294()
				UpdateRoomLights(Camera)
			EndIf
			UpdateDecals()
			UpdateMTF()
			UpdateNPCs()
			UpdateItems()
			UpdateParticles()
			Use427()
			UpdateMonitorSaving()
			;Added a simple code for updating the Particles function depending on the FPSFactor (still WIP, might not be the final version of it) - ENDSHN
			UpdateParticles_Time# = Min(1,UpdateParticles_Time#+FPSfactor)
			If UpdateParticles_Time#=1
				UpdateDevilEmitters()
				UpdateParticles_Devil()
				UpdateParticles_Time#=0
			EndIf
		EndIf
		
		If InfiniteStamina% Then Stamina = Min(100, Stamina + (100.0-Stamina)*0.01*FPSfactor)
		
		If FPSfactor=0
			UpdateWorld(0)
		Else
			UpdateWorld()
			ManipulateNPCBones()
		EndIf
		RenderWorld2()
		
		BlurVolume = Min(CurveValue(0.0, BlurVolume, 20.0),0.95)
		If BlurTimer > 0.0 Then
			BlurVolume = Max(Min(0.95, BlurTimer / 1000.0), BlurVolume)
			BlurTimer = Max(BlurTimer - FPSfactor, 0.0)
		EndIf
		
		UpdateBlur(BlurVolume)
		
		Local darkA# = 0.0
		
		If (Not MenuOpen)  Then
			If Sanity < 0 Then
				If RestoreSanity Then Sanity = Min(Sanity + FPSfactor, 0.0)
				If Sanity < (-200) Then 
					darkA = Max(Min((-Sanity - 200) / 700.0, 0.6), darkA)
					If KillTimer => 0 Then 
						HeartBeatVolume = Min(Abs(Sanity+200)/500.0,1.0)
						HeartBeatRate = Max(70 + Abs(Sanity+200)/6.0,HeartBeatRate)
					EndIf
				EndIf
			EndIf
			
			If EyeStuck > 0 Then 
				BlinkTimer = BLINKFREQ
				EyeStuck = Max(EyeStuck-FPSfactor,0)
				
				If EyeStuck < 9000 Then BlurTimer = Max(BlurTimer, (9000-EyeStuck)*0.5)
				If EyeStuck < 6000 Then darkA = Min(Max(darkA, (6000-EyeStuck)/5000.0),1.0)
				If EyeStuck < 9000 And EyeStuck+FPSfactor =>9000 Then 
					Msg = "The eyedrops are causing your eyes to tear up."
					MsgTimer = 70*6
				EndIf
			EndIf
			
			If BlinkTimer < 0 Then
				If BlinkTimer > - 5 Then
					darkA = Max(darkA, Sin(Abs(BlinkTimer * 18.0)))
				ElseIf BlinkTimer > - 15
					darkA = 1.0
				Else
					darkA = Max(darkA, Abs(Sin(BlinkTimer * 18.0)))
				EndIf
				
				If BlinkTimer <= - 20 Then
					;Randomizes the frequency of blinking. Scales with difficulty.
					Select SelectedDifficulty\otherFactors
						Case EASY
							BLINKFREQ = Rnd(490,700)
						Case NORMAL
							BLINKFREQ = Rnd(455,665)
						Case HARD
							BLINKFREQ = Rnd(420,630)
					End Select 
					BlinkTimer = BLINKFREQ
				EndIf
				
				BlinkTimer = BlinkTimer - FPSfactor
			Else
				BlinkTimer = BlinkTimer - FPSfactor * 0.6 * BlinkEffect
				If EyeIrritation > 0 Then BlinkTimer=BlinkTimer-Min(EyeIrritation / 100.0 + 1.0, 4.0) * FPSfactor
				
				darkA = Max(darkA, 0.0)
			EndIf
			
			EyeIrritation = Max(0, EyeIrritation - FPSfactor)
			
			If BlinkEffectTimer > 0 Then
				BlinkEffectTimer = BlinkEffectTimer - (FPSfactor/70)
			Else
				If BlinkEffect <> 1.0 Then BlinkEffect = 1.0
			EndIf
			
			LightBlink = Max(LightBlink - (FPSfactor / 35.0), 0)
			If LightBlink > 0 Then darkA = Min(Max(darkA, LightBlink * Rnd(0.3, 0.8)), 1.0)
			
			If Using294 Then darkA=1.0
			
			If (Not WearingNightVision) Then darkA = Max((1.0-SecondaryLightOn)*0.9, darkA)
			
			If KillTimer < 0 Then
				InvOpen = False
				SelectedItem = Null
				SelectedScreen = Null
				SelectedMonitor = Null
				BlurTimer = Abs(KillTimer*5)
				KillTimer=KillTimer-(FPSfactor*0.8)
				If KillTimer < - 360 Then 
					MenuOpen = True 
					If SelectedEnding <> -1 Then EndingTimer = Min(KillTimer,-0.1)
				EndIf
				darkA = Max(darkA, Min(Abs(KillTimer / 400.0), 1.0))
			EndIf
			
			If FallTimer < 0 Then
				If SelectedItem <> Null Then
					If Instr(SelectedItem\itemtemplate\tempname,"hazmatsuit") Lor Instr(SelectedItem\itemtemplate\tempname,"vest") Then
						If WearingHazmat=0 And WearingVest=0 Then
							DropItem(SelectedItem)
						EndIf
					EndIf
				EndIf
				InvOpen = False
				SelectedItem = Null
				SelectedScreen = Null
				SelectedMonitor = Null
				BlurTimer = Abs(FallTimer*10)
				FallTimer = FallTimer-FPSfactor
				darkA = Max(darkA, Min(Abs(FallTimer / 400.0), 1.0))				
			EndIf
			
			If SelectedItem <> Null Then
				If SelectedItem\itemtemplate\tempname = "navigator" Lor SelectedItem\itemtemplate\tempname = "nav" Then darkA = Max(darkA, 0.5)
			EndIf
			If SelectedScreen <> Null Then darkA = Max(darkA, 0.5)
			
			EntityAlpha(Dark, darkA)	
		EndIf
		
		If LightFlash > 0 Then
			ShowEntity Light
			EntityAlpha(Light, Max(Min(LightFlash + Rnd(-0.2, 0.2), 1.0), 0.0))
			EntityColor Light,255,255,255
			LightFlash = Max(LightFlash - (FPSfactor / 70.0), 0)
		Else
			HideEntity Light
		EndIf
		
		If KeyHit(KEY_INV) And VomitTimer >= 0 Then
			If (Not UnableToMove) And (Not IsZombie) And (Not Using294) And KillTimer >= 0.0 And SelectedEnding = -1 Then
				Local W$ = ""
				Local V# = 0
				If SelectedItem<>Null
					W$ = SelectedItem\itemtemplate\tempname
					V# = SelectedItem\state
				EndIf
				If (W<>"vest" And W<>"finevest" And W<>"hazmatsuit" And W<>"hazmatsuit2" And W<>"hazmatsuit3") Lor V=0 Lor V=100
					If InvOpen Then
						ResumeSounds()
						MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
					Else
						PauseSounds()
					EndIf
					InvOpen = Not InvOpen
					If OtherOpen<>Null Then OtherOpen=Null
					SelectedItem = Null
				EndIf
			EndIf
		EndIf
		
		If KeyHit(KEY_SAVE) Then
			If SelectedDifficulty\saveType = SAVEANYWHERE Then
				RN$ = PlayerRoom\RoomTemplate\Name$
				If RN$ = "173" Lor (RN$ = "exit1" And EntityY(Collider)>1040.0*RoomScale) Lor RN$ = "gatea"
					Msg = "You cannot save in this location."
					MsgTimer = 70 * 4
				ElseIf (Not CanSave) Lor QuickLoadPercent > -1
					Msg = "You cannot save at this moment."
					MsgTimer = 70 * 4
					If QuickLoadPercent > -1
						Msg = Msg + " (game is loading)"
					EndIf
				Else
					SaveGame(SavePath + CurrSave + "\")
				EndIf
			ElseIf SelectedDifficulty\saveType = SAVEONSCREENS
				If SelectedScreen=Null And SelectedMonitor=Null Then
					Msg = "Saving is only permitted on clickable monitors scattered throughout the facility."
					MsgTimer = 70 * 4
				Else
					RN$ = PlayerRoom\RoomTemplate\Name$
					If RN$ = "173" Lor (RN$ = "exit1" And EntityY(Collider)>1040.0*RoomScale) Lor RN$ = "gatea"
						Msg = "You cannot save in this location."
						MsgTimer = 70 * 4
					ElseIf (Not CanSave) Lor QuickLoadPercent > -1
						Msg = "You cannot save at this moment."
						MsgTimer = 70 * 4
						If QuickLoadPercent > -1
							Msg = Msg + " (game is loading)"
						EndIf
					Else
						If SelectedScreen<>Null
							GameSaved = False
							Playable = True
							DropSpeed = 0
						EndIf
						SaveGame(SavePath + CurrSave + "\")
					EndIf
				EndIf
			Else
				Msg = "Quick saving is disabled."
				MsgTimer = 70 * 4
			EndIf
		ElseIf SelectedDifficulty\saveType = SAVEONSCREENS And (SelectedScreen<>Null Lor SelectedMonitor<>Null)
			If (Msg<>"Game progress saved." And Msg<>"You cannot save in this location."And Msg<>"You cannot save at this moment.") Lor MsgTimer<=0 Then
				Msg = "Press "+KeyName[KEY_SAVE]+" to save."
				MsgTimer = 70*4
			EndIf
			
			If MouseHit2 Then SelectedMonitor = Null
		EndIf
		
		If KeyHit(KEY_CONSOLE) Then
			If CanOpenConsole
				If ConsoleOpen Then
					UsedConsole = True
					ResumeSounds()
					MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
				Else
					PauseSounds()
				EndIf
				ConsoleOpen = (Not ConsoleOpen)
				FlushKeys()
			EndIf
		EndIf
		
		DrawGUI()
		
		If EndingTimer < 0 Then
			If SelectedEnding <> -1 Then DrawEnding()
		Else
			If SelectedEnding = -1 Then DrawMenu()			
		EndIf
		
		UpdateConsole()
		
		If PlayerRoom <> Null Then
			If PlayerRoom\RoomTemplate\Name = "173" Then
				For e.Events = Each Events
					If e\EventName = "173" Then
						If e\EventState3 => 40 And e\EventState3 < 50 Then
							If InvOpen Then
								Msg = "Double click on the document to view it."
								MsgTimer=70*7
								e\EventState3 = 50
							EndIf
						EndIf
						Exit
					EndIf
				Next
			EndIf
		EndIf
		
		If MsgTimer > 0 Then
			Local temp% = False
			If (Not InvOpen%)
				If SelectedItem <> Null
					If SelectedItem\itemtemplate\tempname = "paper" Lor SelectedItem\itemtemplate\tempname = "oldpaper"
						temp% = True
					EndIf
				EndIf
			EndIf
			
			If (Not temp%)
				Color 0,0,0
				AAText((GraphicWidth / 2)+1, (GraphicHeight / 2) + 201, Msg, True, False, Min(MsgTimer / 2, 255)/255.0)
				Color 255,255,255
				AAText((GraphicWidth / 2), (GraphicHeight / 2) + 200, Msg, True, False, Min(MsgTimer / 2, 255)/255.0)
			Else
				Color 0,0,0
				AAText((GraphicWidth / 2)+1, (GraphicHeight * 0.94) + 1, Msg, True, False, Min(MsgTimer / 2, 255)/255.0)
				Color 255,255,255
				AAText((GraphicWidth / 2), (GraphicHeight * 0.94), Msg, True, False, Min(MsgTimer / 2, 255)/255.0)
			EndIf
			MsgTimer=MsgTimer-FPSfactor2 
		EndIf
		
		Color 255, 255, 255
		If ShowFPS Then AASetFont ConsoleFont : AAText 20, 20, "FPS: " + FPS : AASetFont Font1
		
		DrawQuickLoading()
		
		UpdateAchievementMsg()
	EndIf
	
	UpdateScreenGamma()
	
	CatchErrors("Main loop / uncaught")
	
	If Vsync = 0 Then
		Flip 0
	Else 
		Flip 1
	EndIf
Forever

Function Kill()
	If GodMode Then Return
	
	If BreathCHN <> 0 Then
		If ChannelPlaying(BreathCHN) Then StopChannel(BreathCHN)
	EndIf
	
	If KillTimer >= 0 Then
		KillAnim = Rand(0,1)
		PlaySound_Strict(DamageSFX[0])
		If SelectedDifficulty\permaDeath Then
			DeleteFile(CurrentDir() + SavePath + CurrSave+"\save.txt") 
			DeleteDir(SavePath + CurrSave)
			LoadSaveGames()
		EndIf
		
		KillTimer = Min(-1, KillTimer)
		ShowEntity Head
		PositionEntity(Head, EntityX(Camera, True), EntityY(Camera, True), EntityZ(Camera, True), True)
		ResetEntity (Head)
		RotateEntity(Head, 0, EntityYaw(Camera), 0)		
	EndIf
End Function

; ~ Ending IDs Constants
;[Block]
Const Ending_A1% = 0
Const Ending_A2% = 1
Const Ending_B1% = 2
Const Ending_B2% = 3
;[End Block]

Function DrawEnding()
	ShowPointer()
	
	FPSfactor = 0
	If EndingTimer>-2000
		EndingTimer=Max(EndingTimer-FPSfactor2,-1111)
	Else
		EndingTimer=EndingTimer-FPSfactor2
	EndIf
	
	GiveAchievement(Achv055)
	If (Not UsedConsole) Then GiveAchievement(AchvConsole)
	If SelectedDifficulty\name = "Keter" Then GiveAchievement(AchvKeter)
	
	Local x,y,width,height, temp
	Local itt.ItemTemplates, r.Rooms
	
	Select SelectedEnding
		Case Ending_B2, Ending_A1
			ClsColor Max(255+(EndingTimer)*2.8,0), Max(255+(EndingTimer)*2.8,0), Max(255+(EndingTimer)*2.8,0)
		Default
			ClsColor 0,0,0
	End Select
	
	ShouldPlay = 66
	
	Cls
	
	If EndingTimer<-200 Then
		If BreathCHN <> 0 Then
			If ChannelPlaying(BreathCHN) Then StopChannel BreathCHN : Stamina = 100
		EndIf
		
		If EndingScreen = 0 Then
			EndingScreen = LoadImage_Strict("GFX\menu\endingscreen.pt")
			
			ShouldPlay = 23
			CurrMusicVolume = MusicVolume
			
			CurrMusicVolume = MusicVolume
			StopStream_Strict(MusicCHN)
			MusicCHN = StreamSound_Strict("SFX\Music\"+Music[23]+".ogg",CurrMusicVolume,0)
			NowPlaying = ShouldPlay
			
			PlaySound_Strict LightSFX
		EndIf
		
		If EndingTimer > -700 Then 
			If Rand(1,150)<Min((Abs(EndingTimer)-200),155) Then
				DrawImage EndingScreen, GraphicWidth/2-400, GraphicHeight/2-400
			Else
				Color 0,0,0
				Rect 100,100,GraphicWidth-200,GraphicHeight-200
				Color 255,255,255
			EndIf
			
			If EndingTimer+FPSfactor2 > -450 And EndingTimer <= -450 Then
				PlaySound_Strict(LoadTempSound("SFX\Ending\Ending" + (SelectedEnding + 1) + ".ogg"))
			EndIf			
		Else
			DrawImage EndingScreen, GraphicWidth/2-400, GraphicHeight/2-400
			
			If EndingTimer < -1000 And EndingTimer > -2000
				width = ImageWidth(PauseMenuIMG)
				height = ImageHeight(PauseMenuIMG)
				x = GraphicWidth / 2 - width / 2
				y = GraphicHeight / 2 - height / 2
				
				DrawImage PauseMenuIMG, x, y
				
				Color(255, 255, 255)
				AASetFont Font2
				AAText(x + width / 2 + 40*MenuScale, y + 20*MenuScale, "THE END", True)
				AASetFont Font1
				
				If AchievementsMenu=0 Then 
					x = x+132*MenuScale
					y = y+122*MenuScale
					
					Local roomamount = 0, roomsfound = 0
					
					For r.Rooms = Each Rooms
						roomamount = roomamount + 1
						roomsfound = roomsfound + r\found
					Next
					
					Local docamount=0, docsfound=0
					
					For itt.ItemTemplates = Each ItemTemplates
						If itt\tempname = "paper" Then
							docamount=docamount+1
							docsfound=docsfound+itt\found
						EndIf
					Next
					
					Local scpsEncountered=1
					
					For i = 0 To 24
						scpsEncountered = scpsEncountered+Achievements[i]
					Next
					
					Local achievementsUnlocked =0
					
					For i = 0 To MAXACHIEVEMENTS-1
						achievementsUnlocked = achievementsUnlocked + Achievements[i]
					Next
					
					AAText x, y, "SCPs encountered: " +scpsEncountered
					AAText x, y+20*MenuScale, "Achievements unlocked: " + achievementsUnlocked+"/"+(MAXACHIEVEMENTS)
					AAText x, y+40*MenuScale, "Rooms found: " + roomsfound+"/"+roomamount
					AAText x, y+60*MenuScale, "Documents discovered: " +docsfound+"/"+docamount
					AAText x, y+80*MenuScale, "Items refined in SCP-914: " +RefinedItems			
					
					x = GraphicWidth / 2 - width / 2
					y = GraphicHeight / 2 - height / 2
					x = x+width/2
					y = y+height-100*MenuScale
					
					If DrawButton(x-145*MenuScale,y-200*MenuScale,390*MenuScale,60*MenuScale,"ACHIEVEMENTS", True) Then
						AchievementsMenu = 1
					EndIf
					
					If DrawButton(x-145*MenuScale,y-100*MenuScale,390*MenuScale,60*MenuScale,"MAIN MENU", True)
						ShouldPlay = 24
						NowPlaying = ShouldPlay
						For i=0 To 9
							If TempSounds[i]<>0 Then FreeSound_Strict TempSounds[i] : TempSounds[i]=0
						Next
						StopStream_Strict(MusicCHN)
						MusicCHN = StreamSound_Strict("SFX\Music\"+Music[NowPlaying]+".ogg",0.0,Mode)
						SetStreamVolume_Strict(MusicCHN,1.0*MusicVolume)
						FlushKeys()
						EndingTimer=-2000
						InitCredits()
					EndIf
				Else
					ShouldPlay = 23
					DrawMenu()
				EndIf
			;Credits
			ElseIf EndingTimer<=-2000
				ShouldPlay = 24
				DrawCredits()
			EndIf
		EndIf
	EndIf
	If Fullscreen Then DrawImage CursorIMG, ScaledMouseX(),ScaledMouseY()
	
	AASetFont Font1
End Function

Type CreditsLine
	Field txt$
	Field id%
	Field stay%
End Type

Global CreditsTimer# = 0.0
Global CreditsScreen%

Function InitCredits()
	Local cl.CreditsLine
	Local file% = OpenFile("Credits.txt")
	Local l$
	
	CreditsFont% = LoadFont_Strict("GFX\font\cour\Courier New.ttf", 21)
	CreditsFont2% = LoadFont_Strict("GFX\font\courbd\Courier New.ttf", 35)
	
	If CreditsScreen = 0
		CreditsScreen = LoadImage_Strict("GFX\menu\creditsscreen.pt")
	EndIf
	
	Repeat
		l = ReadLine(file)
		cl = New CreditsLine
		cl\txt = l
	Until Eof(file)
	
	Delete First CreditsLine
	CreditsTimer = 0
End Function

Function DrawCredits()
	Local credits_Y# = (EndingTimer+2000)/2+(GraphicHeight+10)
	Local cl.CreditsLine
	Local id%
	Local endlinesamount%
	Local LastCreditLine.CreditsLine
	
	Cls
	
	If Rand(1,300)>1
		DrawImage CreditsScreen, GraphicWidth/2-400, GraphicHeight/2-400
	EndIf
	
	id = 0
	endlinesamount = 0
	LastCreditLine = Null
	Color 255,255,255
	For cl = Each CreditsLine
		cl\id = id
		If Left(cl\txt,1)="*"
			SetFont CreditsFont2
			If cl\stay=False
				Text GraphicWidth/2,credits_Y+(24*cl\id*MenuScale),Right(cl\txt,Len(cl\txt)-1),True
			EndIf
		ElseIf Left(cl\txt,1)="/"
			LastCreditLine = Before(cl)
		Else
			SetFont CreditsFont
			If cl\stay=False
				Text GraphicWidth/2,credits_Y+(24*cl\id*MenuScale),cl\txt,True
			EndIf
		EndIf
		If LastCreditLine<>Null
			If cl\id>LastCreditLine\id
				cl\stay = True
			EndIf
		EndIf
		If cl\stay
			endlinesamount=endlinesamount+1
		EndIf
		id=id+1
	Next
	If (credits_Y+(24*LastCreditLine\id*MenuScale))<-StringHeight(LastCreditLine\txt)
		CreditsTimer=CreditsTimer+(0.5*FPSfactor2)
		If CreditsTimer>=0.0 And CreditsTimer<255.0
			Color Max(Min(CreditsTimer,255),0),Max(Min(CreditsTimer,255),0),Max(Min(CreditsTimer,255),0)
		ElseIf CreditsTimer>=255.0
			Color 255,255,255
			If CreditsTimer>500.0
				CreditsTimer=-255.0
			EndIf
		Else
			Color Max(Min(-CreditsTimer,255),0),Max(Min(-CreditsTimer,255),0),Max(Min(-CreditsTimer,255),0)
			If CreditsTimer>=-1.0
				CreditsTimer=-1.0
			EndIf
		EndIf
		DebugLog CreditsTimer
	EndIf
	If CreditsTimer<>0.0
		For cl = Each CreditsLine
			If cl\stay
				SetFont CreditsFont
				If Left(cl\txt,1)="/"
					Text GraphicWidth/2,(GraphicHeight/2)+(endlinesamount/2)+(24*cl\id*MenuScale),Right(cl\txt,Len(cl\txt)-1),True
				Else
					Text GraphicWidth/2,(GraphicHeight/2)+(24*(cl\id-LastCreditLine\id)*MenuScale)-((endlinesamount/2)*24*MenuScale),cl\txt,True
				EndIf
			EndIf
		Next
	EndIf
	
	If GetKey() Then CreditsTimer=-1
	
	If CreditsTimer=-1
		FreeFont CreditsFont
		FreeFont CreditsFont2
		FreeImage CreditsScreen
		CreditsScreen = 0
		FreeImage EndingScreen
		EndingScreen = 0
		Delete Each CreditsLine
		NullGame(False)
		StopStream_Strict(MusicCHN)
		ShouldPlay = 21
		MenuOpen = False
		MainMenuOpen = True
		MainMenuTab = 0
		CurrSave = ""
		FlushKeys()
	EndIf
End Function

Function MovePlayer()
	CatchErrors("Uncaught (MovePlayer)")
	
	Local Sprint# = 1.0, Speed# = 0.018, i%, angle#
	
	If SuperMan Then
		Speed = Speed * 3
		
		SuperManTimer=SuperManTimer+FPSfactor
		
		CameraShake = Sin(SuperManTimer / 5.0) * (SuperManTimer / 1500.0)
		
		If SuperManTimer > 70 * 50 Then
			DeathMSG = "A Class D jumpsuit found in [DATA REDACTED]. Upon further examination, the jumpsuit was found to be filled with 12.5 kilograms of blue ash-like substance. "
			DeathMSG = DeathMSG + "Chemical analysis of the substance remains non-conclusive. Most likely related to SCP-914."
			Kill()
			ShowEntity Fog
		Else
			BlurTimer = 500		
			HideEntity Fog
		EndIf
	EndIf
	
	If DeathTimer > 0 Then
		DeathTimer=DeathTimer-FPSfactor
		If DeathTimer < 1 Then DeathTimer = -1.0
	ElseIf DeathTimer < 0 
		Kill()
	EndIf
	
	If CurrSpeed > 0 Then
		Stamina = Min(Stamina + 0.15 * FPSfactor/1.25, 100.0)
	Else
		Stamina = Min(Stamina + 0.15 * FPSfactor*1.25, 100.0)
	EndIf
	
	If StaminaEffectTimer > 0 Then
		StaminaEffectTimer = StaminaEffectTimer - (FPSfactor/70)
	Else
		If StaminaEffect <> 1.0 Then StaminaEffect = 1.0
	EndIf
	
	Local temp#
	
	If PlayerRoom\RoomTemplate\Name<>"pocketdimension" Then 
		If KeyDown(KEY_SPRINT) Then
			If Stamina < 5 Then
				temp = 0
				If WearingGasMask>0 Lor Wearing1499>0 Then temp=1
				If ChannelPlaying(BreathCHN)=False Then BreathCHN = PlaySound_Strict(BreathSFX((temp), 0))
			ElseIf Stamina < 50
				If BreathCHN=0 Then
					temp = 0
					If WearingGasMask>0 Lor Wearing1499>0 Then temp=1
					BreathCHN = PlaySound_Strict(BreathSFX((temp), Rand(1,3)))
					ChannelVolume BreathCHN, Min((70.0-Stamina)/70.0,1.0)*SFXVolume
				Else
					If ChannelPlaying(BreathCHN)=False Then
						temp = 0
						If WearingGasMask>0 Lor Wearing1499>0 Then temp=1
						BreathCHN = PlaySound_Strict(BreathSFX((temp), Rand(1,3)))
						ChannelVolume BreathCHN, Min((70.0-Stamina)/70.0,1.0)*SFXVolume			
					EndIf
				EndIf
			EndIf
		EndIf
	EndIf
	
	For i = 0 To MaxItemAmount-1
		If Inventory[i]<>Null Then
			If Inventory[i]\itemtemplate\tempname = "finevest" Then Stamina = Min(Stamina, 60)
		EndIf
	Next
	
	If Wearing714 Then
		Stamina = Min(Stamina, 10)
		Sanity = Max(-850, Sanity)
	EndIf
	
	If IsZombie Then Crouch = False
	
	If Abs(CrouchState-Crouch)<0.001 Then 
		CrouchState = Crouch
	Else
		CrouchState = CurveValue(Crouch, CrouchState, 10.0)
	EndIf
	
	If (Not NoClip) Then 
		If ((KeyDown(KEY_DOWN) Lor KeyDown(KEY_UP)) Lor (KeyDown(KEY_RIGHT) Lor KeyDown(KEY_LEFT)) And Playable) Lor ForceMove>0 Then
			If Crouch = 0 And (KeyDown(KEY_SPRINT)) And Stamina > 0.0 And (Not IsZombie) Then
				Sprint = 2.5
				Stamina = Stamina - FPSfactor * 0.4 * StaminaEffect
				If Stamina <= 0 Then Stamina = -20.0
			EndIf
			
			If PlayerRoom\RoomTemplate\Name = "pocketdimension" Then 
				If EntityY(Collider)<2000*RoomScale Lor EntityY(Collider)>2608*RoomScale Then
					Stamina = 0
					Speed = 0.015
					Sprint = 1.0					
				EndIf
			EndIf	
			
			If ForceMove>0 Then Speed=Speed*ForceMove
			
			If SelectedItem<>Null Then
				If SelectedItem\itemtemplate\tempname = "firstaid" Lor SelectedItem\itemtemplate\tempname = "finefirstaid" Lor SelectedItem\itemtemplate\tempname = "firstaid2" Then 
					Sprint = 0
				EndIf
			EndIf
			
			temp# = (Shake Mod 360)
			
			Local tempchn%
			
			If (Not UnableToMove%) Then Shake# = (Shake + FPSfactor * Min(Sprint, 1.5) * 7) Mod 720
			If temp < 180 And (Shake Mod 360) >= 180 And KillTimer>=0 Then
				If CurrStepSFX=0 Then
					temp = GetStepSound(Collider)
					
					If Sprint = 1.0 Then
						PlayerSoundVolume = Max(4.0,PlayerSoundVolume)
						tempchn% = PlaySound_Strict(StepSFX(temp, 0, Rand(0, 7)))
						ChannelVolume tempchn, (1.0-(Crouch*0.6))*SFXVolume#
					Else
						PlayerSoundVolume = Max(2.5-(Crouch*0.6),PlayerSoundVolume)
						tempchn% = PlaySound_Strict(StepSFX(temp, 1, Rand(0, 7)))
						ChannelVolume tempchn, (1.0-(Crouch*0.6))*SFXVolume#
					EndIf
				ElseIf CurrStepSFX=1
					tempchn% = PlaySound_Strict(Step2SFX[Rand(0, 2)])
					ChannelVolume tempchn, (1.0-(Crouch*0.4))*SFXVolume#
				ElseIf CurrStepSFX=2
					tempchn% = PlaySound_Strict(Step2SFX[Rand(3,5)])
					ChannelVolume tempchn, (1.0-(Crouch*0.4))*SFXVolume#
				ElseIf CurrStepSFX=3
					If Sprint = 1.0 Then
						PlayerSoundVolume = Max(4.0,PlayerSoundVolume)
						tempchn% = PlaySound_Strict(StepSFX(0, 0, Rand(0, 7)))
						ChannelVolume tempchn, (1.0-(Crouch*0.6))*SFXVolume#
					Else
						PlayerSoundVolume = Max(2.5-(Crouch*0.6),PlayerSoundVolume)
						tempchn% = PlaySound_Strict(StepSFX(0, 1, Rand(0, 7)))
						ChannelVolume tempchn, (1.0-(Crouch*0.6))*SFXVolume#
					EndIf
				EndIf
				
			EndIf	
		EndIf
	Else ;noclip on
		If (KeyDown(KEY_SPRINT)) Then 
			Sprint = 2.5
		ElseIf KeyDown(KEY_CROUCH)
			Sprint = 0.5
		EndIf
	EndIf
	
	If KeyHit(KEY_CROUCH) And Playable Then Crouch = (Not Crouch)
	
	Local temp2# = (Speed * Sprint) / (1.0+CrouchState)
	
	If NoClip Then 
		Shake = 0
		CurrSpeed = 0
		CrouchState = 0
		Crouch = 0
		
		RotateEntity Collider, WrapAngle(EntityPitch(Camera)), WrapAngle(EntityYaw(Camera)), 0
		
		Temp2 = Temp2 * NoClipSpeed
		
		If KeyDown(KEY_DOWN) Then MoveEntity Collider, 0, 0, -Temp2*FPSfactor
		If KeyDown(KEY_UP) Then MoveEntity Collider, 0, 0, Temp2*FPSfactor
		
		If KeyDown(KEY_LEFT) Then MoveEntity Collider, -Temp2*FPSfactor, 0, 0
		If KeyDown(KEY_RIGHT) Then MoveEntity Collider, Temp2*FPSfactor, 0, 0	
		
		ResetEntity Collider
	Else
		Temp2# = Temp2 / Max((Injuries+3.0)/3.0,1.0)
		If Injuries > 0.5 Then 
			Temp2 = Temp2*Min((Sin(Shake/2)+1.2),1.0)
		EndIf
		
		temp = False
		If (Not IsZombie%)
			If KeyDown(KEY_DOWN) And Playable Then
				temp = True 
				angle = 180
				If KeyDown(KEY_LEFT) Then angle = 135 
				If KeyDown(KEY_RIGHT) Then angle = -135 
			ElseIf (KeyDown(KEY_UP) And Playable) Then
				temp = True
				angle = 0
				If KeyDown(KEY_LEFT) Then angle = 45 
				If KeyDown(KEY_RIGHT) Then angle = -45 
			ElseIf ForceMove>0 Then
				temp=True
				angle = ForceAngle
			ElseIf Playable Then
				If KeyDown(KEY_LEFT) Then angle = 90 : temp = True
				If KeyDown(KEY_RIGHT) Then angle = -90 : temp = True 
			EndIf
		Else
			temp=True
			angle = ForceAngle
		EndIf
		
		angle = WrapAngle(EntityYaw(Collider,True)+angle+90.0)
		
		If temp Then 
			CurrSpeed = CurveValue(Temp2, CurrSpeed, 20.0)
		Else
			CurrSpeed = Max(CurveValue(0.0, CurrSpeed-0.1, 1.0),0.0)
		EndIf
		
		If (Not UnableToMove%) Then TranslateEntity Collider, Cos(angle)*CurrSpeed * FPSfactor, 0, Sin(angle)*CurrSpeed * FPSfactor, True
		
		Local CollidedFloor% = False
		
		For i = 1 To CountCollisions(Collider)
			If CollisionY(Collider, i) < EntityY(Collider) - 0.25 Then CollidedFloor = True
		Next
		
		If CollidedFloor = True Then
			If DropSpeed# < - 0.07 Then 
				If CurrStepSFX=0 Then
					PlaySound_Strict(StepSFX(GetStepSound(Collider), 0, Rand(0, 7)))
				ElseIf CurrStepSFX=1
					PlaySound_Strict(Step2SFX[Rand(0, 2)])
				ElseIf CurrStepSFX=2
					PlaySound_Strict(Step2SFX[Rand(3, 5)])
				ElseIf CurrStepSFX=3
					PlaySound_Strict(StepSFX(0, 0, Rand(0, 7)))
				EndIf
				PlayerSoundVolume = Max(3.0,PlayerSoundVolume)
			EndIf
			DropSpeed# = 0
		Else
			If PlayerFallingPickDistance#<>0.0
				Local pick = LinePick(EntityX(Collider),EntityY(Collider),EntityZ(Collider),0,-PlayerFallingPickDistance,0)
				If pick
					DropSpeed# = Min(Max(DropSpeed - 0.006 * FPSfactor, -2.0), 0.0)
				Else
					DropSpeed# = 0
				EndIf
			Else
				DropSpeed# = Min(Max(DropSpeed - 0.006 * FPSfactor, -2.0), 0.0)
			EndIf
		EndIf
		PlayerFallingPickDistance# = 10.0
		
		If (Not UnableToMove%) And ShouldEntitiesFall Then TranslateEntity Collider, 0, DropSpeed * FPSfactor, 0
	EndIf
	
	ForceMove = False
	
	If Injuries > 1.0 Then
		Temp2 = Bloodloss
		BlurTimer = Max(Max(Sin(MilliSecs2()/100.0)*Bloodloss*30.0,Bloodloss*2*(2.0-CrouchState)),BlurTimer)
		If (Not I_427\Using And I_427\Timer < 70*360) Then
			Bloodloss = Min(Bloodloss + (Min(Injuries,3.5)/300.0)*FPSfactor,100)
		EndIf
		
		If Temp2 <= 60 And Bloodloss > 60 Then
			Msg = "You are feeling faint from the amount of blood you have lost."
			MsgTimer = 70*4
		EndIf
	EndIf
	
	Update008()
	
	If Bloodloss > 0 Then
		If Rnd(200)<Min(Injuries,4.0) Then
			pvt = CreatePivot()
			PositionEntity pvt, EntityX(Collider)+Rnd(-0.05,0.05),EntityY(Collider)-0.05,EntityZ(Collider)+Rnd(-0.05,0.05)
			TurnEntity pvt, 90, 0, 0
			EntityPick(pvt,0.3)
			de.decals = CreateDecal(Rand(15,16), PickedX(), PickedY()+0.005, PickedZ(), 90, Rand(360), 0)
			de\size = Rnd(0.03,0.08)*Min(Injuries,3.0) : EntityAlpha(de\obj, 1.0) : ScaleSprite de\obj, de\size, de\size
			tempchn% = PlaySound_Strict (DripSFX[Rand(0,2)])
			ChannelVolume tempchn, Rnd(0.0,0.8)*SFXVolume
			ChannelPitch tempchn, Rand(20000,30000)
			
			FreeEntity pvt
		EndIf
		
		CurrCameraZoom = Max(CurrCameraZoom, (Sin(Float(MilliSecs2())/20.0)+1.0)*Bloodloss*0.2)
		
		If Bloodloss > 60 Then Crouch = True
		If Bloodloss => 100 Then 
			Kill()
			HeartBeatVolume = 0.0
		ElseIf Bloodloss > 80.0
			HeartBeatRate = Max(150-(Bloodloss-80)*5,HeartBeatRate)
			HeartBeatVolume = Max(HeartBeatVolume, 0.75+(Bloodloss-80.0)*0.0125)	
		ElseIf Bloodloss > 35.0
			HeartBeatRate = Max(70+Bloodloss,HeartBeatRate)
			HeartBeatVolume = Max(HeartBeatVolume, (Bloodloss-35.0)/60.0)			
		EndIf
	EndIf
	
	If HealTimer > 0 Then
		DebugLog HealTimer
		HealTimer = HealTimer - (FPSfactor / 70)
		Bloodloss = Min(Bloodloss + (2 / 400.0) * FPSfactor, 100)
		Injuries = Max(Injuries - (FPSfactor / 70) / 30, 0.0)
	EndIf
		
	If Playable Then
		If KeyHit(KEY_BLINK) Then BlinkTimer = 0
		If KeyDown(KEY_BLINK) And BlinkTimer < - 10 Then BlinkTimer = -10
	EndIf
	
	If HeartBeatVolume > 0 Then
		If HeartBeatTimer <= 0 Then
			tempchn = PlaySound_Strict (HeartBeatSFX)
			ChannelVolume tempchn, HeartBeatVolume*SFXVolume#
			
			HeartBeatTimer = 70.0*(60.0/Max(HeartBeatRate,1.0))
		Else
			HeartBeatTimer = HeartBeatTimer - FPSfactor
		EndIf
		
		HeartBeatVolume = Max(HeartBeatVolume - FPSfactor*0.05, 0)
	EndIf
	
	CatchErrors("MovePlayer")
End Function

Function MouseLook()
	Local i%
	
	CameraShake = Max(CameraShake - (FPSfactor / 10), 0)
	CameraZoom(Camera, Min(1.0+(CurrCameraZoom/400.0),1.1))
	CurrCameraZoom = Max(CurrCameraZoom - FPSfactor, 0)
	
	If KillTimer >= 0 And FallTimer >=0 Then
		HeadDropSpeed = 0
		
		Local up# = (Sin(Shake) / (20.0+CrouchState*20.0))*0.6	
		Local roll# = Max(Min(Sin(Shake/2)*2.5*Min(Injuries+0.25,3.0),8.0),-8.0)
		
		PositionEntity(Camera, EntityX(Collider) + side, EntityY(Collider) + up + 0.6 + CrouchState * (-0.3), EntityZ(Collider))
		RotateEntity Camera, 0, EntityYaw(Collider), roll*0.5
		
		; -- Update the smoothing que To smooth the movement of the mouse.
		mouse_x_speed_1# = CurveValue(MouseXSpeed() * (MouseSens + 0.6) , mouse_x_speed_1, (6.0 / (MouseSens + 1.0))*MouseSmooth) 
		If PrevFPSFactor>0 Then
			If Abs(FPSfactor/PrevFPSFactor-1.0)>1.0 Then
				;lag spike detected - stop all camera movement
				mouse_x_speed_1 = 0.0
				mouse_y_speed_1 = 0.0
			EndIf
		EndIf
		If IsNaN(mouse_y_speed_1) Then
			mouse_x_speed_1 = 0.0
			mouse_y_speed_1 = 0.0
		EndIf
		If InvertMouse Then
			mouse_y_speed_1# = CurveValue(-MouseYSpeed() * (MouseSens + 0.6), mouse_y_speed_1, (6.0/(MouseSens+1.0))*MouseSmooth) 
		Else
			mouse_y_speed_1# = CurveValue(MouseYSpeed () * (MouseSens + 0.6), mouse_y_speed_1, (6.0/(MouseSens+1.0))*MouseSmooth) 
		EndIf
		
		Local the_yaw# = ((mouse_x_speed_1#)) * mouselook_x_inc# / (1.0+WearingVest)
		Local the_pitch# = ((mouse_y_speed_1#)) * mouselook_y_inc# / (1.0+WearingVest)
		
		TurnEntity Collider, 0.0, -the_yaw#, 0.0 ; Turn the user on the Y (yaw) axis.
		user_camera_pitch# = user_camera_pitch# + the_pitch#
		; -- Limit the user;s camera To within 180 degrees of pitch rotation. ;EntityPitch(); returns useless values so we need To use a variable To keep track of the camera pitch.
		If user_camera_pitch# > 70.0 Then user_camera_pitch# = 70.0
		If user_camera_pitch# < - 70.0 Then user_camera_pitch# = -70.0
		
		RotateEntity Camera, WrapAngle(user_camera_pitch + Rnd(-CameraShake, CameraShake)), WrapAngle(EntityYaw(Collider) + Rnd(-CameraShake, CameraShake)), roll ; Pitch the user;s camera up And down.
		
		If PlayerRoom\RoomTemplate\Name = "pocketdimension" Then
			If EntityY(Collider)<2000*RoomScale Lor EntityY(Collider)>2608*RoomScale Then
				RotateEntity Camera, WrapAngle(EntityPitch(Camera)),WrapAngle(EntityYaw(Camera)), roll+WrapAngle(Sin(MilliSecs2()/150.0)*30.0) ; Pitch the user;s camera up And down.
			EndIf
		EndIf
	Else
		HideEntity Collider
		PositionEntity Camera, EntityX(Head), EntityY(Head), EntityZ(Head)
		
		Local CollidedFloor% = False
		
		For i = 1 To CountCollisions(Head)
			If CollisionY(Head, i) < EntityY(Head) - 0.01 Then CollidedFloor = True
		Next
		
		If CollidedFloor = True Then
			HeadDropSpeed# = 0
		Else
			If KillAnim = 0 Then 
				MoveEntity Head, 0, 0, HeadDropSpeed
				RotateEntity(Head, CurveAngle(-90.0, EntityPitch(Head), 20.0), EntityYaw(Head), EntityRoll(Head))
				RotateEntity(Camera, CurveAngle(EntityPitch(Head) - 40.0, EntityPitch(Camera), 40.0), EntityYaw(Camera), EntityRoll(Camera))
			Else
				MoveEntity Head, 0, 0, -HeadDropSpeed
				RotateEntity(Head, CurveAngle(90.0, EntityPitch(Head), 20.0), EntityYaw(Head), EntityRoll(Head))
				RotateEntity(Camera, CurveAngle(EntityPitch(Head) + 40.0, EntityPitch(Camera), 40.0), EntityYaw(Camera), EntityRoll(Camera))
			EndIf
			HeadDropSpeed# = HeadDropSpeed - 0.002 * FPSfactor
		EndIf
		
		If InvertMouse Then
			TurnEntity (Camera, -MouseYSpeed() * 0.05 * FPSfactor, -MouseXSpeed() * 0.15 * FPSfactor, 0)
		Else
			TurnEntity (Camera, MouseYSpeed() * 0.05 * FPSfactor, -MouseXSpeed() * 0.15 * FPSfactor, 0)
		EndIf
	EndIf
	
	If ParticleAmount=2
		If Rand(35) = 1 Then
			Local pvt% = CreatePivot()
			PositionEntity(pvt, EntityX(Camera, True), EntityY(Camera, True), EntityZ(Camera, True))
			RotateEntity(pvt, 0, Rnd(360), 0)
			If Rand(2) = 1 Then
				MoveEntity(pvt, 0, Rnd(-0.5, 0.5), Rnd(0.5, 1.0))
			Else
				MoveEntity(pvt, 0, Rnd(-0.5, 0.5), Rnd(0.5, 1.0))
			EndIf
			
			Local p.Particles = CreateParticle(EntityX(pvt), EntityY(pvt), EntityZ(pvt), 2, 0.002, 0, 300)
			p\speed = 0.001
			RotateEntity(p\pvt, Rnd(-20, 20), Rnd(360), 0)
			
			p\SizeChange = -0.00001
			
			FreeEntity pvt
		EndIf
	EndIf
	
	; -- Limit the mouse;s movement. Using this method produces smoother mouselook movement than centering the mouse Each loop.
	If (MouseX() > mouse_right_limit) Lor (MouseX() < mouse_left_limit) Lor (MouseY() > mouse_bottom_limit) Lor (MouseY() < mouse_top_limit)
		MoveMouse viewport_center_x, viewport_center_y
	EndIf
	
	If WearingGasMask Lor WearingHazmat Lor Wearing1499 Then
		If Wearing714 = False Then
			If WearingGasMask = 2 Lor Wearing1499 = 2 Lor WearingHazmat = 2 Then
				Stamina = Min(100, Stamina + (100.0-Stamina)*0.01*FPSfactor)
			EndIf
		EndIf
		If WearingHazmat = 1 Then
			Stamina = Min(60, Stamina)
		EndIf
		
		ShowEntity(GasMaskOverlay)
	Else
		HideEntity(GasMaskOverlay)
	EndIf
	
	If (Not WearingNightVision=0) Then
		ShowEntity(NVOverlay)
		If WearingNightVision=2 Then
			EntityColor(NVOverlay, 0,100,255)
			AmbientLightRooms(15)
		ElseIf WearingNightVision=3 Then
			EntityColor(NVOverlay, 255,0,0)
			AmbientLightRooms(15)
		Else
			EntityColor(NVOverlay, 0,255,0)
			AmbientLightRooms(15)
		EndIf
		EntityTexture(Fog, FogNVTexture)
	Else
		AmbientLightRooms(0)
		HideEntity(NVOverlay)
		EntityTexture(Fog, FogTexture)
	EndIf
	
	For i = 0 To 5
		If SCP1025state[i]>0 Then
			Select i
				Case 0 ;common cold
					If FPSfactor>0 Then 
						If Rand(1000)=1 Then
							If CoughCHN = 0 Then
								CoughCHN = PlaySound_Strict(CoughSFX[Rand(0, 2)])
							Else
								If Not ChannelPlaying(CoughCHN) Then CoughCHN = PlaySound_Strict(CoughSFX[Rand(0, 2)])
							EndIf
						EndIf
					EndIf
					Stamina = Stamina - FPSfactor * 0.3
				Case 1 ;chicken pox
					If Rand(9000)=1 Then
						Msg="Your skin is feeling itchy."
						MsgTimer =70*4
					EndIf
				Case 2 ;cancer of the lungs
					If FPSfactor>0 Then 
						If Rand(800)=1 Then
							If CoughCHN = 0 Then
								CoughCHN = PlaySound_Strict(CoughSFX[Rand(0, 2)])
							Else
								If Not ChannelPlaying(CoughCHN) Then CoughCHN = PlaySound_Strict(CoughSFX[Rand(0, 2)])
							EndIf
						EndIf
					EndIf
					Stamina = Stamina - FPSfactor * 0.1
				Case 3 ;appendicitis
					;0.035/sec = 2.1/min
					If (Not I_427\Using And I_427\Timer < 70*360) Then
						SCP1025state[i]=SCP1025state[i]+FPSfactor*0.0005
					EndIf
					If SCP1025state[i]>20.0 Then
						If SCP1025state[i]-FPSfactor<=20.0 Then Msg="The pain in your stomach is becoming unbearable." : MsgTimer = 70*5
						Stamina = Stamina - FPSfactor * 0.3
					ElseIf SCP1025state[i]>10.0
						If SCP1025state[i]-FPSfactor<=10.0 Then Msg="Your stomach is aching." : MsgTimer = 70*5
					EndIf
				Case 4 ;asthma
					If Stamina < 35 Then
						If Rand(Int(140+Stamina*8))=1 Then
							If CoughCHN = 0 Then
								CoughCHN = PlaySound_Strict(CoughSFX[Rand(0, 2)])
							Else
								If Not ChannelPlaying(CoughCHN) Then CoughCHN = PlaySound_Strict(CoughSFX[Rand(0, 2)])
							EndIf
						EndIf
						CurrSpeed = CurveValue(0, CurrSpeed, 10+Stamina*15)
					EndIf
				Case 5;cardiac arrest
					If (Not I_427\Using And I_427\Timer < 70*360) Then
						SCP1025state[i]=SCP1025state[i]+FPSfactor*0.35
					EndIf
					;35/sec
					If SCP1025state[i]>110 Then
						HeartBeatRate=0
						BlurTimer = Max(BlurTimer, 500)
						If SCP1025state[i]>140 Then 
							DeathMSG = Chr(34)+"He died of a cardiac arrest after reading SCP-1025, that's for sure. Is there such a thing as psychosomatic cardiac arrest, or does SCP-1025 have some "
							DeathMSG = DeathMSG + "anomalous properties we are not yet aware of?"+Chr(34)
							Kill()
						EndIf
					Else
						HeartBeatRate=Max(HeartBeatRate, 70+SCP1025state[i])
						HeartBeatVolume = 1.0
					EndIf
			End Select 
		EndIf
	Next
End Function

Function DrawGUI()
	CatchErrors("Uncaught (DrawGUI)")
	
	Local temp%, x%, y%, z%, i%, yawvalue#, pitchvalue#
	Local x2#,y2#,z2#
	Local n%, xtemp, ytemp, strtemp$
	
	Local e.Events, it.Items
	
	If MenuOpen Lor ConsoleOpen Lor SelectedDoor <> Null Lor InvOpen Lor OtherOpen<>Null Lor EndingTimer < 0 Then
		ShowPointer()
	Else
		HidePointer()
	EndIf 	
	
	If PlayerRoom\RoomTemplate\Name = "pocketdimension" Then
		For e.Events = Each Events
			If e\room = PlayerRoom Then
				If Float(e\EventStr)<1000.0 Then
					If e\EventState > 600 Then
						If BlinkTimer < -3 And BlinkTimer > -10 Then
							If e\img = 0 Then
								If BlinkTimer > -5 And Rand(30)=1 Then
									PlaySound_Strict DripSFX[0]
									If e\img = 0 Then e\img = LoadImage_Strict("GFX\npcs\106face.jpg")
								EndIf
							Else
								DrawImage e\img, GraphicWidth/2-Rand(390,310), GraphicHeight/2-Rand(290,310)
							EndIf
						Else
							If e\img <> 0 Then FreeImage e\img : e\img = 0
						EndIf
							
						Exit
					EndIf
				Else
					If BlinkTimer < -3 And BlinkTimer > -10 Then
						If e\img = 0 Then
							If BlinkTimer > -5 Then
								If e\img = 0 Then
									e\img = LoadImage_Strict("GFX\kneelmortal.pd")
									If (ChannelPlaying(e\SoundCHN)) Then
										StopChannel(e\SoundCHN)
									EndIf
									e\SoundCHN = PlaySound_Strict(e\Sound)
								EndIf
							EndIf
						Else
							DrawImage e\img, GraphicWidth/2-Rand(390,310), GraphicHeight/2-Rand(290,310)
						EndIf
					Else
						If e\img <> 0 Then FreeImage e\img : e\img = 0
						If BlinkTimer < -3 Then
							If (Not ChannelPlaying(e\SoundCHN)) Then
								e\SoundCHN = PlaySound_Strict(e\Sound)
							EndIf
						Else
							If (ChannelPlaying(e\SoundCHN)) Then
								StopChannel(e\SoundCHN)
							EndIf
						EndIf
					EndIf
					Exit
				EndIf
			EndIf
		Next
	EndIf
	
	If ClosestButton <> 0 And SelectedDoor = Null And InvOpen = False And MenuOpen = False And OtherOpen = Null Then
		temp% = CreatePivot()
		PositionEntity temp, EntityX(Camera), EntityY(Camera), EntityZ(Camera)
		PointEntity temp, ClosestButton
		yawvalue# = WrapAngle(EntityYaw(Camera) - EntityYaw(temp))
		If yawvalue > 90 And yawvalue <= 180 Then yawvalue = 90
		If yawvalue > 180 And yawvalue < 270 Then yawvalue = 270
		pitchvalue# = WrapAngle(EntityPitch(Camera) - EntityPitch(temp))
		If pitchvalue > 90 And pitchvalue <= 180 Then pitchvalue = 90
		If pitchvalue > 180 And pitchvalue < 270 Then pitchvalue = 270
		
		FreeEntity (temp)
		
		DrawImage(HandIcon, GraphicWidth / 2 + Sin(yawvalue) * (GraphicWidth / 3) - 32, GraphicHeight / 2 - Sin(pitchvalue) * (GraphicHeight / 3) - 32)
		
		If MouseUp1 Then
			MouseUp1 = False
			If ClosestDoor <> Null Then 
				If ClosestDoor\Code <> "" Then
					SelectedDoor = ClosestDoor
				ElseIf Playable Then
					PlaySound2(ButtonSFX, Camera, ClosestButton)
					UseDoor(ClosestDoor,True)				
				EndIf
			EndIf
		EndIf
	EndIf
	
	If ClosestItem <> Null Then
		yawvalue# = -DeltaYaw(Camera, ClosestItem\collider)
		If yawvalue > 90 And yawvalue <= 180 Then yawvalue = 90
		If yawvalue > 180 And yawvalue < 270 Then yawvalue = 270
		pitchvalue# = -DeltaPitch(Camera, ClosestItem\collider)
		If pitchvalue > 90 And pitchvalue <= 180 Then pitchvalue = 90
		If pitchvalue > 180 And pitchvalue < 270 Then pitchvalue = 270
		
		DrawImage(HandIcon2, GraphicWidth / 2 + Sin(yawvalue) * (GraphicWidth / 3) - 32, GraphicHeight / 2 - Sin(pitchvalue) * (GraphicHeight / 3) - 32)
	EndIf
	
	If DrawHandIcon Then DrawImage(HandIcon, GraphicWidth / 2 - 32, GraphicHeight / 2 - 32)
	For i = 0 To 3
		If DrawArrowIcon[i] Then
			x = GraphicWidth / 2 - 32
			y = GraphicHeight / 2 - 32		
			Select i
				Case 0
					y = y - 64 - 5
				Case 1
					x = x + 64 + 5
				Case 2
					y = y + 64 + 5
				Case 3
					x = x - 5 - 64
			End Select
			DrawImage(HandIcon, x, y)
			Color 0, 0, 0
			Rect(x + 4, y + 4, 64 - 8, 64 - 8)
			DrawImage(ArrowIMG[i], x + 21, y + 21)
			DrawArrowIcon[i] = False
		EndIf
	Next
	
	If Using294 Then Use294()
	
	If HUDenabled Then 
		Local width% = 204, height% = 20
		
		x% = 80
		y% = GraphicHeight - 95
		
		Color 255, 255, 255	
		Rect (x, y, width, height, False)
		For i = 1 To Int(((width - 2) * (BlinkTimer / (BLINKFREQ))) / 10)
			DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
		Next	
		Color 0, 0, 0
		Rect(x - 50, y, 30, 30)
		
		If EyeIrritation > 0 Then
			Color 200, 0, 0
			Rect(x - 50 - 3, y - 3, 30 + 6, 30 + 6)
		EndIf
		
		Color 255, 255, 255
		Rect(x - 50 - 1, y - 1, 30 + 2, 30 + 2, False)
		
		DrawImage BlinkIcon, x - 50, y
		
		y = GraphicHeight - 55
		Color 255, 255, 255
		Rect (x, y, width, height, False)
		For i = 1 To Int(((width - 2) * (Stamina / 100.0)) / 10)
			DrawImage(StaminaMeterIMG, x + 3 + 10 * (i - 1), y + 3)
		Next	
		
		Color 0, 0, 0
		Rect(x - 50, y, 30, 30)
		
		Color 255, 255, 255
		Rect(x - 50 - 1, y - 1, 30 + 2, 30 + 2, False)
		If Crouch Then
			DrawImage CrouchIcon, x - 50, y
		Else
			DrawImage SprintIcon, x - 50, y
		EndIf
		
		If DebugHUD Then
			Color 255, 255, 255
			AASetFont ConsoleFont
			
			AAText x - 50, 50, "Player Position: (" + f2s(EntityX(Collider), 3) + ", " + f2s(EntityY(Collider), 3) + ", " + f2s(EntityZ(Collider), 3) + ")"
			AAText x - 50, 70, "Camera Position: (" + f2s(EntityX(Camera), 3)+ ", " + f2s(EntityY(Camera), 3) +", " + f2s(EntityZ(Camera), 3) + ")"
			AAText x - 50, 100, "Player Rotation: (" + f2s(EntityPitch(Collider), 3) + ", " + f2s(EntityYaw(Collider), 3) + ", " + f2s(EntityRoll(Collider), 3) + ")"
			AAText x - 50, 120, "Camera Rotation: (" + f2s(EntityPitch(Camera), 3)+ ", " + f2s(EntityYaw(Camera), 3) +", " + f2s(EntityRoll(Camera), 3) + ")"
			AAText x - 50, 150, "Room: " + PlayerRoom\RoomTemplate\Name
			For ev.Events = Each Events
				If ev\room = PlayerRoom Then
					AAText x - 50, 170, "Room event: " + ev\EventName   
					AAText x - 50, 190, "state: " + ev\EventState
					AAText x - 50, 210, "state2: " + ev\EventState2   
					AAText x - 50, 230, "state3: " + ev\EventState3
					AAText x - 50, 250, "str: "+ ev\EventStr
					Exit
				EndIf
			Next
			AAText x - 50, 280, "Room coordinates: (" + Floor(EntityX(PlayerRoom\obj) / 8.0 + 0.5) + ", " + Floor(EntityZ(PlayerRoom\obj) / 8.0 + 0.5) + ", angle: "+PlayerRoom\angle + ")"
			AAText x - 50, 300, "Stamina: " + f2s(Stamina, 3)
			AAText x - 50, 320, "Death timer: " + f2s(KillTimer, 3)               
			AAText x - 50, 340, "Blink timer: " + f2s(BlinkTimer, 3)
			AAText x - 50, 360, "Injuries: " + Injuries
			AAText x - 50, 380, "Bloodloss: " + Bloodloss
			If Curr173 <> Null
				AAText x - 50, 410, "SCP - 173 Position (collider): (" + f2s(EntityX(Curr173\Collider), 3) + ", " + f2s(EntityY(Curr173\Collider), 3) + ", " + f2s(EntityZ(Curr173\Collider), 3) + ")"
				AAText x - 50, 430, "SCP - 173 Position (obj): (" + f2s(EntityX(Curr173\obj), 3) + ", " + f2s(EntityY(Curr173\obj), 3) + ", " + f2s(EntityZ(Curr173\obj), 3) + ")"
				AAText x - 50, 450, "SCP - 173 State: " + Curr173\State
			EndIf
			If Curr106 <> Null
				AAText x - 50, 470, "SCP - 106 Position: (" + f2s(EntityX(Curr106\obj), 3) + ", " + f2s(EntityY(Curr106\obj), 3) + ", " + f2s(EntityZ(Curr106\obj), 3) + ")"
				AAText x - 50, 490, "SCP - 106 Idle: " + Curr106\Idle
				AAText x - 50, 510, "SCP - 106 State: " + Curr106\State
			EndIf
			offset% = 0
			For npc.NPCs = Each NPCs
				If npc\NPCtype = NPCtype096 Then
					AAText x - 50, 530, "SCP - 096 Position: (" + f2s(EntityX(npc\obj), 3) + ", " + f2s(EntityY(npc\obj), 3) + ", " + f2s(EntityZ(npc\obj), 3) + ")"
					AAText x - 50, 550, "SCP - 096 Idle: " + npc\Idle
					AAText x - 50, 570, "SCP - 096 State: " + npc\State
					AAText x - 50, 590, "SCP - 096 Speed: " + f2s(npc\currspeed, 5)
				EndIf
				If npc\NPCtype = NPCtypeMTF Then
					AAText x - 50, 620 + 60 * offset, "MTF " + offset + " Position: (" + f2s(EntityX(npc\obj), 3) + ", " + f2s(EntityY(npc\obj), 3) + ", " + f2s(EntityZ(npc\obj), 3) + ")"
					AAText x - 50, 640 + 60 * offset, "MTF " + offset + " State: " + npc\State
					AAText x - 50, 660 + 60 * offset, "MTF " + offset + " LastSeen: " + npc\lastseen					
					offset = offset + 1
				EndIf
			Next
			If PlayerRoom\RoomTemplate\Name$ = "dimension1499"
				AAText x + 350, 50, "Current Chunk X/Z: ("+(Int((EntityX(Collider)+20)/40))+", "+(Int((EntityZ(Collider)+20)/40))+")"
				Local CH_Amount% = 0
				For ch.Chunk = Each Chunk
					CH_Amount = CH_Amount + 1
				Next
				AAText x + 350, 70, "Current Chunk Amount: "+CH_Amount
			Else
				AAText x + 350, 50, "Current Room Position: ("+PlayerRoom\x+", "+PlayerRoom\y+", "+PlayerRoom\z+")"
			EndIf
			AAText x + 350, 90, "Video memory: " + ((TotalVidMem() / 1024) - (AvailVidMem() / 1024)) + " MB/" + (TotalVidMem() / 1024) + " MB" + Chr(10)
			AAText x + 350, 110, "Global memory status: " + ((TotalPhys() / 1024) - (AvailPhys() / 1024)) + " MB/" + (TotalPhys() / 1024) + " MB"
			AAText x + 350, 130, "Triangles rendered: "+CurrTrisAmount
			AAText x + 350, 150, "Active textures: "+ActiveTextures()
			AAText x + 350, 170, "SCP-427 state (secs): "+Int(I_427\Timer/70.0)
			AAText x + 350, 190, "SCP-008 infection: "+Infect
			For i = 0 To 5
				AAText x + 350, 210+(20*i), "SCP-1025 State "+i+": "+SCP1025state[i]
			Next
			If SelectedMonitor <> Null Then
				AAText x + 350, 330, "Current monitor: "+SelectedMonitor\ScrObj
			Else
				AAText x + 350, 330, "Current monitor: NULL"
			EndIf
			AASetFont Font1
		EndIf
	EndIf
	
	If SelectedScreen <> Null Then
		DrawImage SelectedScreen\img, GraphicWidth/2-ImageWidth(SelectedScreen\img)/2,GraphicHeight/2-ImageHeight(SelectedScreen\img)/2
		If MouseUp1 Lor MouseHit2 Then
			FreeImage SelectedScreen\img : SelectedScreen\img = 0
			SelectedScreen = Null
			MouseUp1 = False
		EndIf
	EndIf
	
	Local PrevInvOpen% = InvOpen, MouseSlot% = 66
	Local shouldDrawHUD%=True
	
	If SelectedDoor <> Null Then
		SelectedItem = Null
		If shouldDrawHUD Then
			pvt = CreatePivot()
			PositionEntity pvt, EntityX(ClosestButton,True),EntityY(ClosestButton,True),EntityZ(ClosestButton,True)
			RotateEntity pvt, 0, EntityYaw(ClosestButton,True)-180,0
			MoveEntity pvt, 0,0,0.22
			PositionEntity Camera, EntityX(pvt),EntityY(pvt),EntityZ(pvt)
			PointEntity Camera, ClosestButton
			FreeEntity pvt	
			
			CameraProject(Camera, EntityX(ClosestButton,True),EntityY(ClosestButton,True)+MeshHeight(ButtonOBJ)*0.015,EntityZ(ClosestButton,True))
			projY# = ProjectedY()
			CameraProject(Camera, EntityX(ClosestButton,True),EntityY(ClosestButton,True)-MeshHeight(ButtonOBJ)*0.015,EntityZ(ClosestButton,True))
			scale# = (ProjectedY()-projy)/462.0
			
			x = GraphicWidth/2-ImageWidth(KeypadHUD)*scale/2
			y = GraphicHeight/2-ImageHeight(KeypadHUD)*scale/2		
			
			AASetFont Font3
			If KeypadMSG <> "" Then 
				KeypadTimer = KeypadTimer-FPSfactor2
				
				If (KeypadTimer Mod 70) < 35 Then AAText GraphicWidth/2, y+124*scale, KeypadMSG, True,True
				If KeypadTimer =<0 Then
					KeypadMSG = ""
					SelectedDoor = Null
					MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
				EndIf
			Else
				AAText GraphicWidth/2, y+70*scale, "ACCESS CODE: ",True,True	
				AASetFont Font4
				AAText GraphicWidth/2, y+124*scale, KeypadInput,True,True	
			EndIf
			
			x = x+44*scale
			y = y+249*scale
			
			For n = 0 To 3
				For i = 0 To 2
					xtemp = x+Int(58.5*scale*n)
					ytemp = y+(67*scale)*i
					
					temp = False
					If MouseOn(xtemp,ytemp, 54*scale,65*scale) And KeypadMSG = "" Then
						If MouseUp1 Then 
							PlaySound_Strict ButtonSFX
							
							Select (n+1)+(i*4)
								Case 1,2,3
									KeypadInput=KeypadInput + ((n+1)+(i*4))
								Case 4
									KeypadInput=KeypadInput + "0"
								Case 5,6,7
									KeypadInput=KeypadInput + ((n+1)+(i*4)-1)
								Case 8 ;enter
									If KeypadInput = SelectedDoor\Code Then
										PlaySound_Strict ScannerSFX1
										
										If SelectedDoor\Code = Str(AccessCode) Then
											GiveAchievement(AchvMaynard)
										ElseIf SelectedDoor\Code = "7816"
											GiveAchievement(AchvHarp)
										EndIf									
										
										SelectedDoor\locked = 0
										UseDoor(SelectedDoor,True)
										SelectedDoor = Null
										MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
									Else
										PlaySound_Strict ScannerSFX2
										KeypadMSG = "ACCESS DENIED"
										KeypadTimer = 210
										KeypadInput = ""	
									EndIf
								Case 9,10,11
									KeypadInput=KeypadInput + ((n+1)+(i*4)-2)
								Case 12
									KeypadInput = ""
							End Select 
							
							If Len(KeypadInput)> 4 Then KeypadInput = Left(KeypadInput,4)
						EndIf
					Else
						temp = False
					EndIf
				Next
			Next
			
			If Fullscreen Then DrawImage CursorIMG, ScaledMouseX(),ScaledMouseY()
			
			If MouseHit2 Then
				SelectedDoor = Null
				MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
			EndIf
		Else
			SelectedDoor = Null
		EndIf
	Else
		KeypadInput = ""
		KeypadTimer = 0
		KeypadMSG = ""
	EndIf
	
	If KeyHit(1) And EndingTimer=0 And (Not Using294) And SelectedEnding = -1 Then
		If MenuOpen Lor InvOpen Then
			ResumeSounds()
			If OptionsMenu <> 0 Then SaveOptionsINI()
			MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
		Else
			PauseSounds()
		EndIf
		MenuOpen = (Not MenuOpen)
		
		AchievementsMenu = 0
		OptionsMenu = 0
		QuitMSG = 0
		
		SelectedDoor = Null
		SelectedScreen = Null
		SelectedMonitor = Null
		If SelectedItem <> Null Then
			If Instr(SelectedItem\itemtemplate\tempname,"vest") Lor Instr(SelectedItem\itemtemplate\tempname,"hazmatsuit") Then
				If (Not WearingVest) And (Not WearingHazmat) Then
					DropItem(SelectedItem)
				EndIf
				SelectedItem = Null
			EndIf
		EndIf
	EndIf
	
	Local spacing%
	Local PrevOtherOpen.Items
	Local OtherSize%,OtherAmount%
	Local isEmpty%
	Local isMouseOn%
	Local closedInv%
	
	If OtherOpen<>Null Then
		If (PlayerRoom\RoomTemplate\Name = "gatea") Then
			HideEntity Fog
			CameraFogRange Camera, 5,30
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 30)
		ElseIf (PlayerRoom\RoomTemplate\Name = "exit1") And (EntityY(Collider)>1040.0*RoomScale)
			HideEntity Fog
			CameraFogRange Camera, 5,45
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 60)
		EndIf
		
		PrevOtherOpen = OtherOpen
		OtherSize=OtherOpen\invSlots
		
		For i%=0 To OtherSize-1
			If OtherOpen\SecondInv[i] <> Null Then
				OtherAmount = OtherAmount+1
			EndIf
		Next
		
		InvOpen = False
		SelectedDoor = Null
		Local tempX% = 0
		
		width = 70
		height = 70
		spacing% = 35
		
		x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
		y = GraphicHeight / 2 - (height * OtherSize /5 + spacing * (OtherSize / 5 - 1)) / 2;height
		
		ItemAmount = 0
		For  n% = 0 To OtherSize - 1
			isMouseOn% = False
			If ScaledMouseX() > x And ScaledMouseX() < x + width Then
				If ScaledMouseY() > y And ScaledMouseY() < y + height Then
					isMouseOn = True
				EndIf
			EndIf
			
			If isMouseOn Then
				MouseSlot = n
				Color 255, 0, 0
				Rect(x - 1, y - 1, width + 2, height + 2)
			EndIf
			
			DrawFrame(x, y, width, height, (x Mod 64), (x Mod 64))
			
			If OtherOpen = Null Then Exit
			
			If OtherOpen\SecondInv[n] <> Null Then
				If (SelectedItem <> OtherOpen\SecondInv[n] Lor isMouseOn) Then DrawImage(OtherOpen\SecondInv[n]\invimg, x + width / 2 - 32, y + height / 2 - 32)
			EndIf
			DebugLog "otheropen: "+(OtherOpen<>Null)
			If OtherOpen\SecondInv[n] <> Null And SelectedItem <> OtherOpen\SecondInv[n] Then
				If isMouseOn Then
					Color 255, 255, 255	
					AAText(x + width / 2, y + height + spacing - 15, OtherOpen\SecondInv[n]\itemtemplate\name, True)				
					If SelectedItem = Null Then
						If MouseHit1 Then
							SelectedItem = OtherOpen\SecondInv[n]
							MouseHit1 = False
							
							If DoubleClick Then
								If OtherOpen\SecondInv[n]\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[OtherOpen\SecondInv[n]\itemtemplate\sound])
								OtherOpen = Null
								closedInv=True
								InvOpen = False
								DoubleClick = False
							EndIf
						EndIf
					EndIf
				EndIf
				ItemAmount=ItemAmount+1
			Else
				If isMouseOn And MouseHit1 Then
					For z% = 0 To OtherSize - 1
						If OtherOpen\SecondInv[z] = SelectedItem Then OtherOpen\SecondInv[z] = Null
					Next
					OtherOpen\SecondInv[n] = SelectedItem
				EndIf
			EndIf					
			
			x=x+width + spacing
			tempX=tempX + 1
			If tempX = 5 Then 
				tempX=0
				y = y + height*2 
				x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
			EndIf
		Next
		
		If SelectedItem <> Null Then
			If MouseDown1 Then
				If MouseSlot = 66 Then
					MaskImage SelectedItem\invimg,255,0,255
					DrawImage(SelectedItem\invimg, ScaledMouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, ScaledMouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
					MaskImage SelectedItem\invimg,255,0,255
				ElseIf SelectedItem <> PrevOtherOpen\SecondInv[MouseSlot]
					MaskImage SelectedItem\invimg,255,0,255
					DrawImage(SelectedItem\invimg, ScaledMouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, ScaledMouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
					MaskImage SelectedItem\invimg,255,0,255
				EndIf
			Else
				If MouseSlot = 66 Then
					If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
					ShowEntity(SelectedItem\collider)
					PositionEntity(SelectedItem\collider, EntityX(Camera), EntityY(Camera), EntityZ(Camera))
					RotateEntity(SelectedItem\collider, EntityPitch(Camera), EntityYaw(Camera), 0)
					MoveEntity(SelectedItem\collider, 0, -0.1, 0.1)
					RotateEntity(SelectedItem\collider, 0, Rand(360), 0)
					ResetEntity (SelectedItem\collider)
					SelectedItem\DropSpeed = 0.0
					SelectedItem\Picked = False
					For z% = 0 To OtherSize - 1
						If OtherOpen\SecondInv[z] = SelectedItem Then OtherOpen\SecondInv[z] = Null
					Next
					
					isEmpty=True
					If OtherOpen\itemtemplate\tempname = "wallet" Then
						If (Not isEmpty) Then
							For z% = 0 To OtherSize - 1
								If OtherOpen\SecondInv[z]<>Null
									Local name$=OtherOpen\SecondInv[z]\itemtemplate\tempname
									If name$<>"25ct" And name$<>"coin" And name$<>"key" And name$<>"scp860" And name$<>"scp714" Then
										isEmpty=False
										Exit
									EndIf
								EndIf
							Next
						EndIf
					Else
						For z% = 0 To OtherSize - 1
							If OtherOpen\SecondInv[z]<>Null
								isEmpty = False
								Exit
							EndIf
						Next
					EndIf
					
					If isEmpty Then
						Select OtherOpen\itemtemplate\tempname
							Case "clipboard"
								OtherOpen\invimg = OtherOpen\itemtemplate\invimg2
								SetAnimTime OtherOpen\model,17.0
							Case "wallet"
								SetAnimTime OtherOpen\model,0.0
						End Select
					EndIf
					
					SelectedItem = Null
					OtherOpen = Null
					closedInv=True
					
					MoveMouse viewport_center_x, viewport_center_y
				Else
					If PrevOtherOpen\SecondInv[MouseSlot] = Null Then
						For z% = 0 To OtherSize - 1
							If PrevOtherOpen\SecondInv[z] = SelectedItem Then PrevOtherOpen\SecondInv[z] = Null
						Next
						PrevOtherOpen\SecondInv[MouseSlot] = SelectedItem
						SelectedItem = Null
					ElseIf PrevOtherOpen\SecondInv[MouseSlot] <> SelectedItem
						Select SelectedItem\itemtemplate\tempname
							Default
								Msg = "You cannot combine these two items."
								MsgTimer = 70 * 5
						End Select					
					EndIf
				EndIf
				SelectedItem = Null
			EndIf
		EndIf
		
		If Fullscreen Then DrawImage CursorIMG,ScaledMouseX(),ScaledMouseY()
		If (closedInv) And (Not InvOpen) Then 
			ResumeSounds() 
			OtherOpen=Null
			MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
		EndIf
	ElseIf InvOpen Then
		If (PlayerRoom\RoomTemplate\Name = "gatea") Then
			HideEntity Fog
			CameraFogRange Camera, 5,30
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 30)
		ElseIf (PlayerRoom\RoomTemplate\Name = "exit1") And (EntityY(Collider)>1040.0*RoomScale)
			HideEntity Fog
			CameraFogRange Camera, 5,45
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 60)
		EndIf
		
		SelectedDoor = Null
		
		width% = 70
		height% = 70
		spacing% = 35
		
		x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
		y = GraphicHeight / 2 - height
		
		ItemAmount = 0
		For n% = 0 To MaxItemAmount - 1
			isMouseOn% = False
			If ScaledMouseX() > x And ScaledMouseX() < x + width Then
				If ScaledMouseY() > y And ScaledMouseY() < y + height Then
					isMouseOn = True
				EndIf
			EndIf
			
			If Inventory[n] <> Null Then
				Color 200, 200, 200
				Select Inventory[n]\itemtemplate\tempname 
					Case "gasmask"
						If WearingGasMask=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "supergasmask"
						If WearingGasMask=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "gasmask3"
						If WearingGasMask=3 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "hazmatsuit"
						If WearingHazmat=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "hazmatsuit2"
						If WearingHazmat=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "hazmatsuit3"
						If WearingHazmat=3 Then Rect(x - 3, y - 3, width + 6, height + 6)	
					Case "vest"
						If WearingVest=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "finevest"
						If WearingVest=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "scp714"
						If Wearing714=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "nvgoggles"
						If WearingNightVision=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "supernv"
						If WearingNightVision=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "scp1499"
						If Wearing1499=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "super1499"
						If Wearing1499=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "finenvgoggles"
						If WearingNightVision=3 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "scp427"
						If I_427\Using=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
				End Select
			EndIf
			
			If isMouseOn Then
				MouseSlot = n
				Color 255, 0, 0
				Rect(x - 1, y - 1, width + 2, height + 2)
			EndIf
			
			Color 255, 255, 255
			DrawFrame(x, y, width, height, (x Mod 64), (x Mod 64))
			
			If Inventory[n] <> Null Then
				If (SelectedItem <> Inventory[n] Lor isMouseOn) Then 
					DrawImage(Inventory[n]\invimg, x + width / 2 - 32, y + height / 2 - 32)
				EndIf
			EndIf
			
			If Inventory[n] <> Null And SelectedItem <> Inventory[n] Then
				If isMouseOn Then
					If SelectedItem = Null Then
						If MouseHit1 Then
							SelectedItem = Inventory[n]
							MouseHit1 = False
							
							If DoubleClick Then
								If WearingHazmat > 0 And Instr(SelectedItem\itemtemplate\tempname,"hazmatsuit")=0 Then
									Msg = "You cannot use any items while wearing a hazmat suit."
									MsgTimer = 70*5
									SelectedItem = Null
									Return
								EndIf
								If Inventory[n]\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[Inventory[n]\itemtemplate\sound])
								InvOpen = False
								DoubleClick = False
							EndIf
						EndIf
						AASetFont Font1
						Color 0,0,0
						AAText(x + width / 2 + 1, y + height + spacing - 15 + 1, Inventory[n]\name, True)							
						Color 255, 255, 255	
						AAText(x + width / 2, y + height + spacing - 15, Inventory[n]\name, True)	
					EndIf
				EndIf
				
				ItemAmount=ItemAmount+1
			Else
				If isMouseOn And MouseHit1 Then
					For z% = 0 To MaxItemAmount - 1
						If Inventory[z] = SelectedItem Then Inventory[z] = Null
					Next
					Inventory[n] = SelectedItem
				EndIf
			EndIf					
			
			x=x+width + spacing
			If n = 4 Then 
				y = y + height*2 
				x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
			EndIf
		Next
		
		If SelectedItem <> Null Then
			If MouseDown1 Then
				If MouseSlot = 66 Then
					DrawImage(SelectedItem\invimg, ScaledMouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, ScaledMouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
				ElseIf SelectedItem <> Inventory[MouseSlot]
					DrawImage(SelectedItem\invimg, ScaledMouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, ScaledMouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
				EndIf
			Else
				If MouseSlot = 66 Then
					Select SelectedItem\itemtemplate\tempname
						Case "vest","finevest","hazmatsuit","hazmatsuit2","hazmatsuit3"
							Msg = "Double click on this item to take it off."
							MsgTimer = 70*5
						Case "scp1499","super1499"
							If Wearing1499>0 Then
								Msg = "Double click on this item to take it off."
								MsgTimer = 70*5
							Else
								DropItem(SelectedItem)
								SelectedItem = Null
								InvOpen = False
							EndIf
						Default
							DropItem(SelectedItem)
							SelectedItem = Null
							InvOpen = False
					End Select
					
					MoveMouse viewport_center_x, viewport_center_y
				Else
					If Inventory[MouseSlot] = Null Then
						For z% = 0 To MaxItemAmount - 1
							If Inventory[z] = SelectedItem Then Inventory[z] = Null
						Next
						Inventory[MouseSlot] = SelectedItem
						SelectedItem = Null
					ElseIf Inventory[MouseSlot] <> SelectedItem
						Select SelectedItem\itemtemplate\tempname
							Case "paper","key1","key2","key3","key4","key5","key6","misc","oldpaper","badge","ticket","25ct","coin","key","scp860"
								;[Block]
								If Inventory[MouseSlot]\itemtemplate\tempname = "clipboard" Then
									;Add an item to clipboard
									Local added.Items = Null
									Local b$ = SelectedItem\itemtemplate\tempname
									Local b2$ = SelectedItem\itemtemplate\name
									If (b<>"misc" And b<>"25ct" And b<>"coin" And b<>"key" And b<>"scp860" And b<>"scp714") Lor (b2="Playing Card" Lor b2="Mastercard") Then
										For c% = 0 To Inventory[MouseSlot]\invSlots-1
											If (Inventory[MouseSlot]\SecondInv[c] = Null)
												If SelectedItem <> Null Then
													Inventory[MouseSlot]\SecondInv[c] = SelectedItem
													Inventory[MouseSlot]\state = 1.0
													SetAnimTime Inventory[MouseSlot]\model,0.0
													Inventory[MouseSlot]\invimg = Inventory[MouseSlot]\itemtemplate\invimg
													
													For ri% = 0 To MaxItemAmount - 1
														If Inventory[ri] = SelectedItem Then
															Inventory[ri] = Null
															PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
														EndIf
													Next
													added = SelectedItem
													SelectedItem = Null : Exit
												EndIf
											EndIf
										Next
										If SelectedItem <> Null Then
											Msg = "The paperclip is not strong enough to hold any more items."
										Else
											If added\itemtemplate\tempname = "paper" Lor added\itemtemplate\tempname = "oldpaper" Then
												Msg = "This document was added to the clipboard."
											ElseIf added\itemtemplate\tempname = "badge"
												Msg = added\itemtemplate\name + " was added to the clipboard."
											Else
												Msg = "The " + added\itemtemplate\name + " was added to the clipboard."
											EndIf
											
										EndIf
										MsgTimer = 70 * 5
									Else
										Msg = "You cannot combine these two items."
										MsgTimer = 70 * 5
									EndIf
								ElseIf Inventory[MouseSlot]\itemtemplate\tempname = "wallet" Then
									;Add an item to clipboard
									added.Items = Null
									b$ = SelectedItem\itemtemplate\tempname
									b2$ = SelectedItem\itemtemplate\name
									If (b<>"misc" And b<>"paper" And b<>"oldpaper") Lor (b2="Playing Card" Lor b2="Mastercard") Then
										For c% = 0 To Inventory[MouseSlot]\invSlots-1
											If (Inventory[MouseSlot]\SecondInv[c] = Null)
												If SelectedItem <> Null Then
													Inventory[MouseSlot]\SecondInv[c] = SelectedItem
													Inventory[MouseSlot]\state = 1.0
													If b<>"25ct" And b<>"coin" And b<>"key" And b<>"scp860"
														SetAnimTime Inventory[MouseSlot]\model,3.0
													EndIf
													Inventory[MouseSlot]\invimg = Inventory[MouseSlot]\itemtemplate\invimg
													
													For ri% = 0 To MaxItemAmount - 1
														If Inventory[ri] = SelectedItem Then
															Inventory[ri] = Null
															PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
														EndIf
													Next
													added = SelectedItem
													SelectedItem = Null : Exit
												EndIf
											EndIf
										Next
										If SelectedItem <> Null Then
											Msg = "The wallet is full."
										Else
											Msg = "You put "+added\itemtemplate\name+" into the wallet."
										EndIf
										
										MsgTimer = 70 * 5
									Else
										Msg = "You cannot combine these two items."
										MsgTimer = 70 * 5
									EndIf
								Else
									Msg = "You cannot combine these two items."
									MsgTimer = 70 * 5
								EndIf
								SelectedItem = Null
								
								;[End Block]
							Case "bat"
								;[Block]
								Select Inventory[MouseSlot]\itemtemplate\name
									Case "S-NAV Navigator", "S-NAV 300 Navigator", "S-NAV 310 Navigator"
										If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])	
										RemoveItem (SelectedItem)
										SelectedItem = Null
										Inventory[MouseSlot]\state = 100.0
										Msg = "You replaced the navigator's battery."
										MsgTimer = 70 * 5
									Case "S-NAV Navigator Ultimate"
										Msg = "There seems to be no place for batteries in this navigator."
										MsgTimer = 70 * 5
									Case "Radio Transceiver"
										Select Inventory[MouseSlot]\itemtemplate\tempname 
											Case "fineradio", "veryfineradio"
												Msg = "There seems to be no place for batteries in this radio."
												MsgTimer = 70 * 5
											Case "18vradio"
												Msg = "The battery doesn't fit inside this radio."
												MsgTimer = 70 * 5
											Case "radio"
												If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])	
												RemoveItem (SelectedItem)
												SelectedItem = Null
												Inventory[MouseSlot]\state = 100.0
												Msg = "You replaced the radio's battery."
												MsgTimer = 70 * 5
										End Select
									Case "Night Vision Goggles"
										Local nvname$ = Inventory[MouseSlot]\itemtemplate\tempname
										If nvname$="nvgoggles" Lor nvname$="supernv" Then
											If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])	
											RemoveItem (SelectedItem)
											SelectedItem = Null
											Inventory[MouseSlot]\state = 1000.0
											Msg = "You replaced the goggles' battery."
											MsgTimer = 70 * 5
										Else
											Msg = "There seems to be no place for batteries in these night vision goggles."
											MsgTimer = 70 * 5
										EndIf
									Default
										Msg = "You cannot combine these two items."
										MsgTimer = 70 * 5	
								End Select
								;[End Block]
							Case "18vbat"
								;[Block]
								Select Inventory[MouseSlot]\itemtemplate\name
									Case "S-NAV Navigator", "S-NAV 300 Navigator", "S-NAV 310 Navigator"
										Msg = "The battery doesn't fit inside this navigator."
										MsgTimer = 70 * 5
									Case "S-NAV Navigator Ultimate"
										Msg = "There seems to be no place for batteries in this navigator."
										MsgTimer = 70 * 5
									Case "Radio Transceiver"
										Select Inventory[MouseSlot]\itemtemplate\tempname 
											Case "fineradio", "veryfineradio"
												Msg = "There seems to be no place for batteries in this radio."
												MsgTimer = 70 * 5		
											Case "18vradio"
												If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])	
												RemoveItem (SelectedItem)
												SelectedItem = Null
												Inventory[MouseSlot]\state = 100.0
												Msg = "You replaced the radio's battery."
												MsgTimer = 70 * 5
										End Select 
									Default
										Msg = "You cannot combine these two items."
										MsgTimer = 70 * 5	
								End Select
								;[End Block]
							Default
								;[Block]
								Msg = "You cannot combine these two items."
								MsgTimer = 70 * 5
								;[End Block]
						End Select					
					EndIf
				EndIf
				SelectedItem = Null
			EndIf
		EndIf
		
		If Fullscreen Then DrawImage CursorIMG, ScaledMouseX(),ScaledMouseY()
		
		If InvOpen = False Then 
			ResumeSounds() 
			MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
		EndIf
	Else
		If SelectedItem <> Null Then
			Select SelectedItem\itemtemplate\tempname
				Case "nvgoggles"
					;[Block]
					If Wearing1499 = 0 And WearingHazmat=0 Then
						If WearingNightVision = 1 Then
							Msg = "You removed the goggles."
							CameraFogFar = StoredCameraFogFar
						Else
							Msg = "You put on the goggles."
							WearingGasMask = 0
							WearingNightVision = 0
							StoredCameraFogFar = CameraFogFar
							CameraFogFar = 30
						EndIf
						
						WearingNightVision = (Not WearingNightVision)
					ElseIf Wearing1499 > 0 Then
						Msg = "You need to take off SCP-1499 in order to put on the goggles."
					Else
						Msg = "You need to take off the hazmat suit in order to put on the goggles."
					EndIf
					SelectedItem = Null
					MsgTimer = 70 * 5
					;[End Block]
				Case "supernv"
					;[Block]
					If Wearing1499 = 0 And WearingHazmat=0 Then
						If WearingNightVision = 2 Then
							Msg = "You removed the goggles."
							CameraFogFar = StoredCameraFogFar
						Else
							Msg = "You put on the goggles."
							WearingGasMask = 0
							WearingNightVision = 0
							StoredCameraFogFar = CameraFogFar
							CameraFogFar = 30
						EndIf
						
						WearingNightVision = (Not WearingNightVision) * 2
					ElseIf Wearing1499 > 0 Then
						Msg = "You need to take off SCP-1499 in order to put on the goggles."
					Else
						Msg = "You need to take off the hazmat suit in order to put on the goggles."
					EndIf
					SelectedItem = Null
					MsgTimer = 70 * 5
					;[End Block]
				Case "finenvgoggles"
					;[Block]
					If Wearing1499 = 0 And WearingHazmat = 0 Then
						If WearingNightVision = 3 Then
							Msg = "You removed the goggles."
							CameraFogFar = StoredCameraFogFar
						Else
							Msg = "You put on the goggles."
							WearingGasMask = 0
							WearingNightVision = 0
							StoredCameraFogFar = CameraFogFar
							CameraFogFar = 30
						EndIf
						
						WearingNightVision = (Not WearingNightVision) * 3
					ElseIf Wearing1499 > 0 Then
						Msg = "You need to take off SCP-1499 in order to put on the goggles."
					Else
						Msg = "You need to take off the hazmat suit in order to put on the goggles."
					EndIf
					SelectedItem = Null
					MsgTimer = 70 * 5
					;[End Block]
				Case "1123"
					;[Block]
					If Not (Wearing714 = 1) Then
						If PlayerRoom\RoomTemplate\Name <> "room1123" Then
							LightFlash = 7
							PlaySound_Strict(LoadTempSound("SFX\SCP\1123\Touch.ogg"))		
							DeathMSG = "Subject D-9341 was shot dead after attempting to attack a member of Nine-Tailed Fox. Surveillance tapes show that the subject had been "
							DeathMSG = DeathMSG + "wandering around the site approximately 9 minutes prior, shouting the phrase " + Chr(34) + "get rid of the four pests" + Chr(34)
							DeathMSG = DeathMSG + " in chinese. SCP-1123 was found in [REDACTED] nearby, suggesting the subject had come into physical contact with it. How "
							DeathMSG = DeathMSG + "exactly SCP-1123 was removed from its containment chamber is still unknown."
							Kill()
							Return
						EndIf
						
						For e.Events = Each Events
							If e\EventName = "room1123" Then 
								If e\EventState = 0 Then
									LightFlash = 3
									PlaySound_Strict(LoadTempSound("SFX\SCP\1123\Touch.ogg"))		
								EndIf
								e\EventState = Max(1, e\EventState)
								Exit
							EndIf
						Next
					EndIf
					;[End Block]
				Case "key1", "key2", "key3", "key4", "key5", "key6", "scp860", "hand", "hand2", "25ct"
					;[Block]
					DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
					;[End Block]
				Case "scp513"
					;[Block]
					PlaySound_Strict LoadTempSound("SFX\SCP\513\Bell1.ogg")
					
					If Curr5131 = Null
						Curr5131 = CreateNPC(NPCtype5131, 0,0,0)
					EndIf	
					SelectedItem = Null
					;[End Block]
				Case "scp500"
					;[Block]
					If CanUseItem(False, False, True)
						GiveAchievement(Achv500)
						
						If Infect > 0 Then
							Msg = "You swallowed the pill. Your nausea is fading."
						Else
							Msg = "You swallowed the pill."
						EndIf
						MsgTimer = 70*7
						
						DeathTimer = 0
						Infect = 0
						Stamina = 100
						For i = 0 To 5
							SCP1025state[i]=0
						Next
						If StaminaEffect > 1.0 Then
							StaminaEffect = 1.0
							StaminaEffectTimer = 0.0
						EndIf
						
						RemoveItem(SelectedItem)
						SelectedItem = Null
					EndIf	
					;[End Block]
				Case "veryfinefirstaid"
					;[Block]
					If CanUseItem(False, False, True)
						Select Rand(5)
							Case 1
								Injuries = 3.5
								Msg = "You started bleeding heavily."
								MsgTimer = 70*7
							Case 2
								Injuries = 0
								Bloodloss = 0
								Msg = "Your wounds are healing up rapidly."
								MsgTimer = 70*7
							Case 3
								Injuries = Max(0, Injuries - Rnd(0.5,3.5))
								Bloodloss = Max(0, Bloodloss - Rnd(10,100))
								Msg = "You feel much better."
								MsgTimer = 70*7
							Case 4
								BlurTimer = 10000
								Bloodloss = 0
								Msg = "You feel nauseated."
								MsgTimer = 70*7
							Case 5
								BlinkTimer = -10
								
								Local roomname$ = PlayerRoom\RoomTemplate\Name
								
								If roomname = "dimension1499" Lor roomname = "gatea" Lor (roomname="exit1" And EntityY(Collider)>1040.0*RoomScale)
									Injuries = 2.5
									Msg = "You started bleeding heavily."
									MsgTimer = 70*7
								Else
									For r.Rooms = Each Rooms
										If r\RoomTemplate\Name = "pocketdimension" Then
											PositionEntity(Collider, EntityX(r\obj),0.8,EntityZ(r\obj))		
											ResetEntity Collider									
											UpdateDoors()
											UpdateRooms()
											PlaySound_Strict(Use914SFX)
											DropSpeed = 0
											Curr106\State = -2500
											Exit
										EndIf
									Next
									Msg = "For some inexplicable reason, you find yourself inside the pocket dimension."
									MsgTimer = 70*8
								EndIf
						End Select
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "firstaid", "finefirstaid", "firstaid2"
					;[Block]
					If Bloodloss = 0 And Injuries = 0 Then
						Msg = "You don't need to use a first aid kit right now."
						MsgTimer = 70*5
						SelectedItem = Null
					Else
						If CanUseItem(False, True, True)
							CurrSpeed = CurveValue(0, CurrSpeed, 5.0)
							Crouch = True
							
							DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
							
							width% = 300
							height% = 20
							x% = GraphicWidth / 2 - width / 2
							y% = GraphicHeight / 2 + 80
							Rect(x, y, width+4, height, False)
							For  i% = 1 To Int((width - 2) * (SelectedItem\state / 100.0) / 10)
								DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
							Next
							
							SelectedItem\state = Min(SelectedItem\state+(FPSfactor/5.0),100)			
							
							If SelectedItem\state = 100 Then
								If SelectedItem\itemtemplate\tempname = "finefirstaid" Then
									Bloodloss = 0
									Injuries = Max(0, Injuries - 2.0)
									If Injuries = 0 Then
										Msg = "You bandaged the wounds and took a painkiller. You feel fine."
									ElseIf Injuries > 1.0
										Msg = "You bandaged the wounds and took a painkiller, but you were not able to stop the bleeding."
									Else
										Msg = "You bandaged the wounds and took a painkiller, but you still feel sore."
									EndIf
									MsgTimer = 70*5
									RemoveItem(SelectedItem)
								Else
									Bloodloss = Max(0, Bloodloss - Rand(10,20))
									If Injuries => 2.5 Then
										Msg = "The wounds were way too severe to staunch the bleeding completely."
										Injuries = Max(2.5, Injuries-Rnd(0.3,0.7))
									ElseIf Injuries > 1.0
										Injuries = Max(0.5, Injuries-Rnd(0.5,1.0))
										If Injuries > 1.0 Then
											Msg = "You bandaged the wounds but were unable to staunch the bleeding completely."
										Else
											Msg = "You managed to stop the bleeding."
										EndIf
									Else
										If Injuries > 0.5 Then
											Injuries = 0.5
											Msg = "You took a painkiller, easing the pain slightly."
										Else
											Injuries = 0.5
											Msg = "You took a painkiller, but it still hurts to walk."
										EndIf
									EndIf
									
									If SelectedItem\itemtemplate\tempname = "firstaid2" Then 
										Select Rand(6)
											Case 1
												SuperMan = True
												Msg = "You have becomed overwhelmedwithadrenalineholyshitWOOOOOO~!"
											Case 2
												InvertMouse = (Not InvertMouse)
												Msg = "You suddenly find it very difficult to turn your head."
											Case 3
												BlurTimer = 5000
												Msg = "You feel nauseated."
											Case 4
												BlinkEffect = 0.6
												BlinkEffectTimer = Rand(20,30)
											Case 5
												Bloodloss = 0
												Injuries = 0
												Msg = "You bandaged the wounds. The bleeding stopped completely and you feel fine."
											Case 6
												Msg = "You bandaged the wounds and blood started pouring heavily through the bandages."
												Injuries = 3.5
										End Select
									EndIf
									MsgTimer = 70*5
									RemoveItem(SelectedItem)
								EndIf							
							EndIf
						EndIf
					EndIf
					;[End Block]
				Case "eyedrops"
					;[Block]
					If CanUseItem(False,False,False)
						BlinkEffect = 0.6
						BlinkEffectTimer = Rand(20,30)
						BlurTimer = 200
						
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "fineeyedrops"
					;[Block]
					If CanUseItem(False,False,False)
						BlinkEffect = 0.4
						BlinkEffectTimer = Rand(30,40)
						Bloodloss = Max(Bloodloss-1.0, 0)
						BlurTimer = 200
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "supereyedrops"
					;[Block]
					If CanUseItem(False,False,False)
						BlinkEffect = 0.0
						BlinkEffectTimer = 60
						EyeStuck = 10000
						BlurTimer = 1000
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "paper", "ticket"
					;[Block]
					If SelectedItem\itemtemplate\img = 0 Then
						Select SelectedItem\itemtemplate\name
							Case "Burnt Note" 
								SelectedItem\itemtemplate\img = LoadImage_Strict("GFX\items\bn.it")
								SetBuffer ImageBuffer(SelectedItem\itemtemplate\img)
								Color 0,0,0
								AAText 277, 469, AccessCode, True, True
								Color 255,255,255
								SetBuffer BackBuffer()
							Case "Document SCP-372"
								SelectedItem\itemtemplate\img = LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
								SelectedItem\itemtemplate\img = ResizeImage2(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
								
								SetBuffer ImageBuffer(SelectedItem\itemtemplate\img)
								Color 37,45,137
								AASetFont Font5
								temp = ((Int(AccessCode)*3) Mod 10000)
								If temp < 1000 Then temp = temp+1000
								AAText 383*MenuScale, 734*MenuScale, temp, True, True
								Color 255,255,255
								SetBuffer BackBuffer()
							Case "Movie Ticket"
								;don't resize because it messes up the masking
								SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
								
								If (SelectedItem\state = 0) Then
									Msg = Chr(34)+"Hey, I remember this movie!"+Chr(34)
									MsgTimer = 70*10
									PlaySound_Strict LoadTempSound("SFX\SCP\1162\NostalgiaCancer"+Rand(1,5)+".ogg")
									SelectedItem\state = 1
								EndIf
							Default 
								SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
								SelectedItem\itemtemplate\img = ResizeImage2(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
						End Select
						
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					DrawImage(SelectedItem\itemtemplate\img, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\img) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\img) / 2)
					;[End Block]
				Case "scp1025"
					;[Block]
					GiveAchievement(Achv1025) 
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\state = Rand(0,5)
						SelectedItem\itemtemplate\img=LoadImage_Strict("GFX\items\1025\1025_"+Int(SelectedItem\state)+".jpg")	
						SelectedItem\itemtemplate\img = ResizeImage2(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
						
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					If (Not Wearing714) Then SCP1025state[SelectedItem\state]=Max(1,SCP1025state[SelectedItem\state])
					
					DrawImage(SelectedItem\itemtemplate\img, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\img) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\img) / 2)
					;[End Block]
				Case "cup"
					;[Block]
					If CanUseItem(False,False,True)
						SelectedItem\name = Trim(Lower(SelectedItem\name))
						If Left(SelectedItem\name, Min(6,Len(SelectedItem\name))) = "cup of" Then
							SelectedItem\name = Right(SelectedItem\name, Len(SelectedItem\name)-7)
						ElseIf Left(SelectedItem\name, Min(8,Len(SelectedItem\name))) = "a cup of" 
							SelectedItem\name = Right(SelectedItem\name, Len(SelectedItem\name)-9)
						EndIf
						
						;the state of refined items is more than 1.0 (fine setting increases it by 1, very fine doubles it)
						x2 = (SelectedItem\state+1.0)
						
						Local iniStr$ = "DATA\SCP-294.ini"
						
						Local loc% = GetINISectionLocation(iniStr, SelectedItem\name)
						
						strtemp = GetINIString2(iniStr, loc, "message")
						If strtemp <> "" Then Msg = strtemp : MsgTimer = 70*6
						
						If GetINIInt2(iniStr, loc, "lethal") Lor GetINIInt2(iniStr, loc, "deathtimer") Then 
							DeathMSG = GetINIString2(iniStr, loc, "deathmessage")
							If GetINIInt2(iniStr, loc, "lethal") Then Kill()
						EndIf
						BlurTimer = GetINIInt2(iniStr, loc, "blur")*70
						If VomitTimer = 0 Then VomitTimer = GetINIInt2(iniStr, loc, "vomit")
						CameraShakeTimer = GetINIString2(iniStr, loc, "camerashake")
						Injuries = Max(Injuries + GetINIInt2(iniStr, loc, "damage"),0)
						Bloodloss = Max(Bloodloss + GetINIInt2(iniStr, loc, "blood loss"),0)
						strtemp =  GetINIString2(iniStr, loc, "sound")
						If strtemp<>"" Then
							PlaySound_Strict LoadTempSound(strtemp)
						EndIf
						If GetINIInt2(iniStr, loc, "stomachache") Then SCP1025state[3]=1
						
						DeathTimer=GetINIInt2(iniStr, loc, "deathtimer")*70
						
						BlinkEffect = Float(GetINIString2(iniStr, loc, "blink effect", 1.0))*x2
						BlinkEffectTimer = Float(GetINIString2(iniStr, loc, "blink effect timer", 1.0))*x2
						
						StaminaEffect = Float(GetINIString2(iniStr, loc, "stamina effect", 1.0))*x2
						StaminaEffectTimer = Float(GetINIString2(iniStr, loc, "stamina effect timer", 1.0))*x2
						
						strtemp = GetINIString2(iniStr, loc, "refusemessage")
						If strtemp <> "" Then
							Msg = strtemp 
							MsgTimer = 70*6		
						Else
							it.Items = CreateItem("Empty Cup", "emptycup", 0,0,0)
							it\Picked = True
							For i = 0 To MaxItemAmount-1
								If Inventory[i]=SelectedItem Then Inventory[i] = it : Exit
							Next					
							EntityType (it\collider, HIT_ITEM)
							
							RemoveItem(SelectedItem)						
						EndIf
						
						SelectedItem = Null
					EndIf
					;[End Block]
				Case "syringe"
					;[Block]
					If CanUseItem(False,True,True)
						HealTimer = 30
						StaminaEffect = 0.5
						StaminaEffectTimer = 20
						
						Msg = "You injected yourself with the syringe and feel a slight adrenaline rush."
						MsgTimer = 70 * 8
						
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "finesyringe"
					;[Block]
					If CanUseItem(False,True,True)
						HealTimer = Rnd(20, 40)
						StaminaEffect = Rnd(0.5, 0.8)
						StaminaEffectTimer = Rnd(20, 30)
						
						Msg = "You injected yourself with the syringe and feel an adrenaline rush."
						MsgTimer = 70 * 8
						
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "veryfinesyringe"
					;[Block]
					If CanUseItem(False,True,True)
						Select Rand(3)
							Case 1
								HealTimer = Rnd(40, 60)
								StaminaEffect = 0.1
								StaminaEffectTimer = 30
								Msg = "You injected yourself with the syringe and feel a huge adrenaline rush."
							Case 2
								SuperMan = True
								Msg = "You injected yourself with the syringe and feel a humongous adrenaline rush."
							Case 3
								VomitTimer = 30
								Msg = "You injected yourself with the syringe and feel a pain in your stomach."
						End Select
						
						MsgTimer = 70 * 8
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "radio","18vradio","fineradio","veryfineradio"
					;[Block]
					If SelectedItem\state <= 100 Then SelectedItem\state = Max(0, SelectedItem\state - FPSfactor * 0.004)
					
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					;RadioState[5] = has the "use the number keys" -message been shown yet (true/false)
					;RadioState[6] = a timer for the "code channel"
					;RadioState[7] = another timer for the "code channel"
					
					If RadioState[5] = 0 Then 
						Msg = "Use the numbered keys 1 through 5 to cycle between various channels."
						MsgTimer = 70 * 5
						RadioState[5] = 1
						RadioState[0] = -1
					EndIf
					
					strtemp$ = ""
					
					x = GraphicWidth - ImageWidth(SelectedItem\itemtemplate\img)
					y = GraphicHeight - ImageHeight(SelectedItem\itemtemplate\img)
					
					DrawImage(SelectedItem\itemtemplate\img, x, y)
					
					If SelectedItem\state > 0 Then
						If PlayerRoom\RoomTemplate\Name = "pocketdimension" Lor CoffinDistance < 4.0 Then
							ResumeChannel(RadioCHN[5])
							If ChannelPlaying(RadioCHN[5]) = False Then RadioCHN[5] = PlaySound_Strict(RadioStatic)	
						Else
							Select Int(SelectedItem\state2)
								Case 0
									ResumeChannel(RadioCHN[0])
									strtemp = "        USER TRACK PLAYER - "
									If (Not EnableUserTracks)
										If ChannelPlaying(RadioCHN[0]) = False Then RadioCHN[0] = PlaySound_Strict(RadioStatic)
										strtemp = strtemp + "NOT ENABLED     "
									ElseIf UserTrackMusicAmount<1
										If ChannelPlaying(RadioCHN[0]) = False Then RadioCHN[0] = PlaySound_Strict(RadioStatic)
										strtemp = strtemp + "NO TRACKS FOUND     "
									Else
										If (Not ChannelPlaying(RadioCHN[0]))
											If (Not UserTrackFlag%)
												If UserTrackMode
													If RadioState[0]<(UserTrackMusicAmount-1)
														RadioState[0] = RadioState[0] + 1
													Else
														RadioState[0] = 0
													EndIf
													UserTrackFlag = True
												Else
													RadioState[0] = Rand(0,UserTrackMusicAmount-1)
												EndIf
											EndIf
											If CurrUserTrack%<>0 Then FreeSound_Strict(CurrUserTrack%) : CurrUserTrack% = 0
											CurrUserTrack% = LoadSound_Strict("SFX\Radio\UserTracks\"+UserTrackName[RadioState[0]])
											RadioCHN[0] = PlaySound_Strict(CurrUserTrack%)
											DebugLog "CurrTrack: "+RadioState[0]
											DebugLog UserTrackName[RadioState[0]]
										Else
											strtemp = strtemp + Upper(UserTrackName[RadioState[0]]) + "          "
											UserTrackFlag = False
										EndIf
										
										If KeyHit(2) Then
											PlaySound_Strict RadioSquelch
											If (Not UserTrackFlag%)
												If UserTrackMode
													If RadioState[0]<(UserTrackMusicAmount-1)
														RadioState[0] = RadioState[0] + 1
													Else
														RadioState[0] = 0
													EndIf
													UserTrackFlag = True
												Else
													RadioState[0] = Rand(0,UserTrackMusicAmount-1)
												EndIf
											EndIf
											If CurrUserTrack%<>0 Then FreeSound_Strict(CurrUserTrack%) : CurrUserTrack% = 0
											CurrUserTrack% = LoadSound_Strict("SFX\Radio\UserTracks\"+UserTrackName[RadioState[0]])
											RadioCHN[0] = PlaySound_Strict(CurrUserTrack%)
											DebugLog "CurrTrack: "+RadioState[0]
											DebugLog UserTrackName[RadioState[0]]
										EndIf
									EndIf
								Case 1
									DebugLog RadioState[1] 
									
									ResumeChannel(RadioCHN[1])
									strtemp = "        WARNING - CONTAINMENT BREACH          "
									If ChannelPlaying(RadioCHN[1]) = False Then
										If RadioState[1] => 5 Then
											RadioCHN[1] = PlaySound_Strict(RadioSFX(1,1))	
											RadioState[1] = 0
										Else
											RadioState[1]=RadioState[1]+1	
											RadioCHN[1] = PlaySound_Strict(RadioSFX(1,0))	
										EndIf
									EndIf
								Case 2 ;scp-radio
									ResumeChannel(RadioCHN[2])
									strtemp = "        SCP Foundation On-Site Radio          "
									If ChannelPlaying(RadioCHN[2]) = False Then
										RadioState[2]=RadioState[2]+1
										If RadioState[2] = 17 Then RadioState[2] = 1
										If Floor(RadioState[2]/2)=Ceil(RadioState[2]/2) Then
											RadioCHN[2] = PlaySound_Strict(RadioSFX(2,Int(RadioState[2]/2)))	
										Else
											RadioCHN[2] = PlaySound_Strict(RadioSFX(2,0))
										EndIf
									EndIf 
								Case 3
									ResumeChannel(RadioCHN[3])
									strtemp = "             EMERGENCY CHANNEL - RESERVED FOR COMMUNICATION IN THE EVENT OF A CONTAINMENT BREACH         "
									If ChannelPlaying(RadioCHN[3]) = False Then RadioCHN[3] = PlaySound_Strict(RadioStatic)
									
									If MTFtimer > 0 Then 
										RadioState[3]=RadioState[3]+Max(Rand(-10,1),0)
										Select RadioState[3]
											Case 40
												If Not RadioState3[0] Then
													RadioCHN[3] = PlaySound_Strict(LoadTempSound("SFX\Character\MTF\Random1.ogg"))
													RadioState[3] = RadioState[3]+1	
													RadioState3[0] = True	
												EndIf											
											Case 400
												If Not RadioState3[1] Then
													RadioCHN[3] = PlaySound_Strict(LoadTempSound("SFX\Character\MTF\Random2.ogg"))
													RadioState[3] = RadioState[3]+1	
													RadioState3[1] = True	
												EndIf	
											Case 800
												If Not RadioState3[2] Then
													RadioCHN[3] = PlaySound_Strict(LoadTempSound("SFX\Character\MTF\Random3.ogg"))
													RadioState[3] = RadioState[3]+1	
													RadioState3[2] = True
												EndIf													
											Case 1200
												If Not RadioState3[3] Then
													RadioCHN[3] = PlaySound_Strict(LoadTempSound("SFX\Character\MTF\Random4.ogg"))	
													RadioState[3] = RadioState[3]+1	
													RadioState3[3] = True
												EndIf
											Case 1600
												If Not RadioState3[4] Then
													RadioCHN[3] = PlaySound_Strict(LoadTempSound("SFX\Character\MTF\Random5.ogg"))	
													RadioState[3] = RadioState[3]+1
													RadioState3[4] = True
												EndIf
											Case 2000
												If Not RadioState3[5] Then
													RadioCHN[3] = PlaySound_Strict(LoadTempSound("SFX\Character\MTF\Random6.ogg"))	
													RadioState[3] = RadioState[3]+1
													RadioState3[5] = True
												EndIf
											Case 2400
												If Not RadioState3[6] Then
													RadioCHN[3] = PlaySound_Strict(LoadTempSound("SFX\Character\MTF\Random7.ogg"))	
													RadioState[3] = RadioState[3]+1
													RadioState3[6] = True
												EndIf
										End Select
									EndIf
								Case 4
									ResumeChannel(RadioCHN[6])
									If ChannelPlaying(RadioCHN[6]) = False Then RadioCHN[6] = PlaySound_Strict(RadioStatic)									
									
									ResumeChannel(RadioCHN[4])
									If ChannelPlaying(RadioCHN[4]) = False Then 
										If RemoteDoorOn = False And RadioState[8] = False Then
											RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter3.ogg"))	
											RadioState[8] = True
										Else
											RadioState[4]=RadioState[4]+Max(Rand(-10,1),0)
											
											Select RadioState[4]
												Case 10
													If (Not Contained106)
														If Not RadioState4[0] Then
															RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\OhGod.ogg"))
															RadioState[4] = RadioState[4]+1
															RadioState4[0] = True
														EndIf
													EndIf
												Case 100
													If Not RadioState4[1] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter2.ogg"))
														RadioState[4] = RadioState[4]+1
														RadioState4[1] = True
													EndIf		
												Case 158
													If MTFtimer = 0 And (Not RadioState4[2]) Then 
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\franklin1.ogg"))
														RadioState[4] = RadioState[4]+1
														RadioState[2] = True
													EndIf
												Case 200
													If Not RadioState4[3] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter4.ogg"))
														RadioState[4] = RadioState[4]+1
														RadioState4[3] = True
													EndIf		
												Case 260
													If Not RadioState4[4] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\SCP\035\RadioHelp1.ogg"))
														RadioState[4] = RadioState[4]+1
														RadioState4[4] = True
													EndIf		
												Case 300
													If Not RadioState4[5] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter1.ogg"))	
														RadioState[4] = RadioState[4]+1	
														RadioState4[5] = True
													EndIf		
												Case 350
													If Not RadioState4[6] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\franklin2.ogg"))
														RadioState[4] = RadioState[4]+1
														RadioState4[6] = True
													EndIf		
												Case 400
													If Not RadioState4[7] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\SCP\035\RadioHelp2.ogg"))
														RadioState[4] = RadioState[4]+1
														RadioState4[7] = True
													EndIf		
												Case 450
													If Not RadioState4[8] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\franklin3.ogg"))	
														RadioState[4] = RadioState[4]+1		
														RadioState4[8] = True
													EndIf		
												Case 600
													If Not RadioState4[9] Then
														RadioCHN[4] = PlaySound_Strict(LoadTempSound("SFX\radio\franklin4.ogg"))	
														RadioState[4] = RadioState[4]+1	
														RadioState4[9] = True
													EndIf		
											End Select
										EndIf
									EndIf
								Case 5
									ResumeChannel(RadioCHN[5])
									If ChannelPlaying(RadioCHN[5]) = False Then RadioCHN[5] = PlaySound_Strict(RadioStatic)
							End Select 
							
							x=x+66
							y=y+419
							
							Color (30,30,30)
							
							If SelectedItem\state <= 100 Then
								For i = 0 To 4
									Rect(x, y+8*i, 43 - i * 6, 4, Ceil(SelectedItem\state / 20.0) > 4 - i )
								Next
							EndIf	
							
							AASetFont Font3
							AAText(x+60, y, "CHN")						
							
							If SelectedItem\itemtemplate\tempname = "veryfineradio" Then
								ResumeChannel(RadioCHN[0])
								If ChannelPlaying(RadioCHN[0]) = False Then RadioCHN[0] = PlaySound_Strict(RadioStatic)
								RadioState[6]=RadioState[6] + FPSfactor
								temp = Mid(Str(AccessCode),RadioState[8]+1,1)
								If RadioState[6]-FPSfactor =< RadioState[7]*50 And RadioState[6]>RadioState[7]*50 Then
									PlaySound_Strict(RadioBuzz)
									RadioState[7]=RadioState[7]+1
									If RadioState[7]=>temp Then
										RadioState[7]=0
										RadioState[6]=-100
										RadioState[8]=RadioState[8]+1
										If RadioState[8]=4 Then RadioState[8]=0 : RadioState[6]=-200
									EndIf
								EndIf
								
								strtemp = ""
								For i = 0 To Rand(5, 30)
									strtemp = strtemp + Chr(Rand(1,100))
								Next
								
								AASetFont Font4
								AAText(x+97, y+16, Rand(0,9),True,True)
							Else
								For i = 2 To 6
									If KeyHit(i) Then
										If SelectedItem\state2 <> i-2 Then
											PlaySound_Strict RadioSquelch
											If RadioCHN[Int(SelectedItem\state2)] <> 0 Then PauseChannel(RadioCHN[Int(SelectedItem\state2)])
										EndIf
										SelectedItem\state2 = i-2
										If RadioCHN[SelectedItem\state2]<>0 Then ResumeChannel(RadioCHN[SelectedItem\state2])
									EndIf
								Next
								AASetFont Font4
								AAText(x+97, y+16, Int(SelectedItem\state2+1),True,True)
							EndIf
							
							AASetFont Font3
							If strtemp <> "" Then
								strtemp = Right(Left(strtemp, (Int(MilliSecs2()/300) Mod Len(strtemp))),10)
								AAText(x+32, y+33, strtemp)
							EndIf
							AASetFont Font1
						EndIf
					EndIf
					;[End Block]
				Case "cigarette"
					;[Block]
					If CanUseItem(False,False,True)
						If SelectedItem\state = 0 Then
							Select Rand(6)
								Case 1
									Msg = Chr(34)+"I don't have anything to light it with. Umm, what about that... Nevermind."+Chr(34)
								Case 2
									Msg = "You are unable to get lit."
								Case 3
									Msg = Chr(34)+"I quit that a long time ago."+Chr(34)
									RemoveItem(SelectedItem)
								Case 4
									Msg = Chr(34)+"Even if I wanted one, I have nothing to light it with."+Chr(34)
								Case 5
									Msg = Chr(34)+"Could really go for one now... Wish I had a lighter."+Chr(34)
								Case 6
									Msg = Chr(34)+"Don't plan on starting, even at a time like this."+Chr(34)
									RemoveItem(SelectedItem)
							End Select
							SelectedItem\state = 1 
						Else
							Msg = "You are unable to get lit."
						EndIf
						MsgTimer = 70 * 5
					EndIf
					;[End Block]
				Case "420"
					;[Block]
					If CanUseItem(False,False,True)
						If Wearing714=1 Then
							Msg = Chr(34) + "DUDE WTF THIS SHIT DOESN'T EVEN WORK" + Chr(34)
						Else
							Msg = Chr(34) + "MAN DATS SUM GOOD ASS SHIT" + Chr(34)
							Injuries = Max(Injuries-0.5, 0)
							BlurTimer = 500
							GiveAchievement(Achv420)
							PlaySound_Strict LoadTempSound("SFX\Music\420J.ogg")
						EndIf
						MsgTimer = 70 * 5
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "420s"
					;[Block]
					If CanUseItem(False,False,True)
						If Wearing714=1 Then
							Msg = Chr(34) + "DUDE WTF THIS SHIT DOESN'T EVEN WORK" + Chr(34)
						Else
							DeathMSG = "Subject D-9341 found in a comatose state in [DATA REDACTED]. The subject was holding what appears to be a cigarette while smiling widely. "
							DeathMSG = DeathMSG+"Chemical analysis of the cigarette has been inconclusive, although it seems to contain a high concentration of an unidentified chemical "
							DeathMSG = DeathMSG+"whose molecular structure is remarkably similar to that of tetrahydrocannabinol."
							Msg = Chr(34) + "UH WHERE... WHAT WAS I DOING AGAIN... MAN I NEED TO TAKE A NAP..." + Chr(34)
							KillTimer = -1						
						EndIf
						MsgTimer = 70 * 6
						RemoveItem(SelectedItem)
					EndIf
					;[End Block]
				Case "scp714"
					;[Block]
					If Wearing714=1 Then
						Msg = "You removed the ring."
						Wearing714 = False
					Else
						GiveAchievement(Achv714)
						Msg = "You put on the ring."
						Wearing714 = True
					EndIf
					MsgTimer = 70 * 5
					SelectedItem = Null	
					;[End Block]
				Case "hazmatsuit", "hazmatsuit2", "hazmatsuit3"
					;[Block]
					If WearingVest = 0 Then
						CurrSpeed = CurveValue(0, CurrSpeed, 5.0)
						
						DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
						
						width% = 300
						height% = 20
						x% = GraphicWidth / 2 - width / 2
						y% = GraphicHeight / 2 + 80
						Rect(x, y, width+4, height, False)
						For  i% = 1 To Int((width - 2) * (SelectedItem\state / 100.0) / 10)
							DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
						Next
						
						SelectedItem\state = Min(SelectedItem\state+(FPSfactor/4.0),100)
						
						If SelectedItem\state=100 Then
							If WearingHazmat>0 Then
								Msg = "You removed the hazmat suit."
								WearingHazmat = False
								DropItem(SelectedItem)
							Else
								If SelectedItem\itemtemplate\tempname="hazmatsuit" Then
									WearingHazmat = 1
								ElseIf SelectedItem\itemtemplate\tempname="hazmatsuit2" Then
									WearingHazmat = 2
								Else
									WearingHazmat = 3
								EndIf
								If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
								Msg = "You put on the hazmat suit."
								If WearingNightVision Then CameraFogFar = StoredCameraFogFar
								WearingGasMask = 0
								WearingNightVision = 0
							EndIf
							SelectedItem\state=0
							MsgTimer = 70 * 5
							SelectedItem = Null
						EndIf
					EndIf
					;[End Block]
				Case "vest","finevest"
					;[Block]
					CurrSpeed = CurveValue(0, CurrSpeed, 5.0)
					
					DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
					
					width% = 300
					height% = 20
					x% = GraphicWidth / 2 - width / 2
					y% = GraphicHeight / 2 + 80
					Rect(x, y, width+4, height, False)
					For  i% = 1 To Int((width - 2) * (SelectedItem\state / 100.0) / 10)
						DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
					Next
					
					SelectedItem\state = Min(SelectedItem\state+(FPSfactor/(2.0+(0.5*(SelectedItem\itemtemplate\tempname="finevest")))),100)
					
					If SelectedItem\state=100 Then
						If WearingVest>0 Then
							Msg = "You removed the vest."
							WearingVest = False
							DropItem(SelectedItem)
						Else
							If SelectedItem\itemtemplate\tempname="vest" Then
								Msg = "You put on the vest and feel slightly encumbered."
								WearingVest = 1
							Else
								Msg = "You put on the vest and feel heavily encumbered."
								WearingVest = 2
							EndIf
							If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
						EndIf
						SelectedItem\state=0
						MsgTimer = 70 * 5
						SelectedItem = Null
					EndIf
					;[End Block]
				Case "gasmask", "supergasmask", "gasmask3"
					;[Block]
					If Wearing1499 = 0 And WearingHazmat = 0 Then
						If WearingGasMask Then
							Msg = "You removed the gas mask."
						Else
							If SelectedItem\itemtemplate\tempname = "supergasmask"
								Msg = "You put on the gas mask and you can breathe easier."
							Else
								Msg = "You put on the gas mask."
							EndIf
							If WearingNightVision Then CameraFogFar = StoredCameraFogFar
							WearingNightVision = 0
							WearingGasMask = 0
						EndIf
						If SelectedItem\itemtemplate\tempname="gasmask3" Then
							If WearingGasMask = 0 Then WearingGasMask = 3 Else WearingGasMask=0
						ElseIf SelectedItem\itemtemplate\tempname="supergasmask"
							If WearingGasMask = 0 Then WearingGasMask = 2 Else WearingGasMask=0
						Else
							WearingGasMask = (Not WearingGasMask)
						EndIf
					ElseIf Wearing1499 > 0 Then
						Msg = "You need to take off SCP-1499 in order to put on the gas mask."
					Else
						Msg = "You need to take off the hazmat suit in order to put on the gas mask."
					EndIf
					SelectedItem = Null
					MsgTimer = 70 * 5
					;[End Block]
				Case "navigator", "nav"
					;[Block]
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					If SelectedItem\state <= 100 Then SelectedItem\state = Max(0, SelectedItem\state - FPSfactor * 0.005)
					
					x = GraphicWidth - ImageWidth(SelectedItem\itemtemplate\img)*0.5+20
					y = GraphicHeight - ImageHeight(SelectedItem\itemtemplate\img)*0.4-85
					width = 287
					height = 256
					
					Local PlayerX,PlayerZ
					
					DrawImage(SelectedItem\itemtemplate\img, x - ImageWidth(SelectedItem\itemtemplate\img) / 2, y - ImageHeight(SelectedItem\itemtemplate\img) / 2 + 85)
					
					AASetFont Font3
					
					Local NavWorks% = True
					
					If PlayerRoom\RoomTemplate\Name$ = "pocketdimension" Lor PlayerRoom\RoomTemplate\Name$ = "dimension1499" Then
						NavWorks% = False
					ElseIf PlayerRoom\RoomTemplate\Name$ = "room860" Then
						For e.Events = Each Events
							If e\EventName = "room860" Then
								If e\EventState = 1.0 Then
									NavWorks% = False
								EndIf
								Exit
							EndIf
						Next
					EndIf
					
					If (Not NavWorks) Then
						If (MilliSecs2() Mod 1000) > 300 Then
							Color(200, 0, 0)
							AAText(x, y + height / 2 - 80, "ERROR 06", True)
							AAText(x, y + height / 2 - 60, "LOCATION UNKNOWN", True)						
						EndIf
					Else
						If SelectedItem\state > 0 And (Rnd(CoffinDistance + 15.0) > 1.0 Lor PlayerRoom\RoomTemplate\Name <> "coffin") Then
							PlayerX% = Floor((EntityX(PlayerRoom\obj)+8) / 8.0 + 0.5)
							PlayerZ% = Floor((EntityZ(PlayerRoom\obj)+8) / 8.0 + 0.5)
							
							SetBuffer ImageBuffer(NavBG)
							
							Local xx = x-ImageWidth(SelectedItem\itemtemplate\img)/2
							Local yy = y-ImageHeight(SelectedItem\itemtemplate\img)/2+85
							
							DrawImage(SelectedItem\itemtemplate\img, xx, yy)
							
							x = x - 12 + (((EntityX(Collider)-4.0)+8.0) Mod 8.0)*3
							y = y + 12 - (((EntityZ(Collider)-4.0)+8.0) Mod 8.0)*3
							For x2 = Max(0, PlayerX - 6) To Min(MapSize, PlayerX + 6)
								For z2 = Max(0, PlayerZ - 6) To Min(MapSize, PlayerZ + 6)
									If CoffinDistance > 16.0 Lor Rnd(16.0)<CoffinDistance Then 
										If MapTemp(x2, z2)>0 And (MapFound(x2, z2) > 0 Lor SelectedItem\itemtemplate\name = "S-NAV 310 Navigator" Lor SelectedItem\itemtemplate\name = "S-NAV Navigator Ultimate") Then
											Local drawx% = x + (PlayerX - 1 - x2) * 24 , drawy% = y - (PlayerZ - 1 - z2) * 24
											
											If x2+1<=MapSize Then
												If MapTemp(x2+1,z2)=False
													DrawImage NavImages[3],drawx-12,drawy-12
												EndIf
											Else
												DrawImage NavImages[3],drawx-12,drawy-12
											EndIf
											If x2-1>=0 Then
												If MapTemp(x2-1,z2)=False
													DrawImage NavImages[1],drawx-12,drawy-12
												EndIf
											Else
												DrawImage NavImages[1],drawx-12,drawy-12
											EndIf
											If z2-1>=0 Then
												If MapTemp(x2,z2-1)=False
													DrawImage NavImages[0],drawx-12,drawy-12
												EndIf
											Else
												DrawImage NavImages[0],drawx-12,drawy-12
											EndIf
											If z2+1<=MapSize Then
												If MapTemp(x2,z2+1)=False
													DrawImage NavImages[2],drawx-12,drawy-12
												EndIf
											Else
												DrawImage NavImages[2],drawx-12,drawy-12
											EndIf
										EndIf
									EndIf
								Next
							Next
							
							SetBuffer BackBuffer()
							DrawImageRect NavBG,xx+80,yy+70,xx+80,yy+70,270,230
							Color 30,30,30
							If SelectedItem\itemtemplate\name = "S-NAV Navigator" Then Color(100, 0, 0)
							Rect xx+80,yy+70,270,230,False
							
							x = GraphicWidth - ImageWidth(SelectedItem\itemtemplate\img)*0.5+20
							y = GraphicHeight - ImageHeight(SelectedItem\itemtemplate\img)*0.4-85
							
							If SelectedItem\itemtemplate\name = "S-NAV Navigator" Then 
								Color(100, 0, 0)
							Else
								Color (30,30,30)
							EndIf
							If (MilliSecs2() Mod 1000) > 300 Then
								If SelectedItem\itemtemplate\name <> "S-NAV 310 Navigator" And SelectedItem\itemtemplate\name <> "S-NAV Navigator Ultimate" Then
									AAText(x - width/2 + 10, y - height/2 + 10, "MAP DATABASE OFFLINE")
								EndIf
								
								yawvalue = EntityYaw(Collider)-90
								x1 = x+Cos(yawvalue)*6 : y1 = y-Sin(yawvalue)*6
								x2 = x+Cos(yawvalue-140)*5 : y2 = y-Sin(yawvalue-140)*5				
								x3 = x+Cos(yawvalue+140)*5 : y3 = y-Sin(yawvalue+140)*5
								
								Line x1,y1,x2,y2
								Line x1,y1,x3,y3
								Line x2,y2,x3,y3
							EndIf
							
							Local SCPs_found% = 0, Dist#
							
							If SelectedItem\itemtemplate\name = "S-NAV Navigator Ultimate" And (MilliSecs2() Mod 600) < 400 Then
								If Curr173<>Null Then
									Dist = EntityDistanceSquared(Camera, Curr173\obj)
									If Dist < 1024.0 Then
										Dist = Sqr(Ceil(Dist / 8.0) * 8.0)
										Color 100, 0, 0
										Oval(x - Dist * 3, y - 7 - Dist * 3, Dist * 3 * 2, Dist * 3 * 2, False)
										AAText(x - width / 2 + 10, y - height / 2 + 30, "SCP-173")
										SCPs_found% = SCPs_found% + 1
									EndIf
								EndIf
								If Curr106<>Null Then
									Dist# = EntityDistanceSquared(Camera, Curr106\obj)
									If Dist < 1024.0 Then
										Dist = Sqr(Dist)
										Color 100, 0, 0
										Oval(x - Dist * 1.5, y - 7 - Dist * 1.5, Dist * 3, Dist * 3, False)
										AAText(x - width / 2 + 10, y - height / 2 + 30 + (20*SCPs_found), "SCP-106")
										SCPs_found% = SCPs_found% + 1
									EndIf
								EndIf
								If Curr096<>Null Then 
									Dist# = EntityDistanceSquared(Camera, Curr096\obj)
									If Dist < 1024.0 Then
										Dist = Sqr(Dist)
										Color 100, 0, 0
										Oval(x - Dist * 1.5, y - 7 - Dist * 1.5, Dist * 3, Dist * 3, False)
										AAText(x - width / 2 + 10, y - height / 2 + 30 + (20*SCPs_found), "SCP-096")
										SCPs_found% = SCPs_found% + 1
									EndIf
								EndIf
								For np.NPCs = Each NPCs
									If np\NPCtype = NPCtype049
										If (Not np\HideFromNVG) Then
											Dist# = EntityDistanceSquared(Camera, np\obj)
											If Dist < 1024.0 Then
												Dist = Sqr(Dist)
												Color 100, 0, 0
												Oval(x - Dist * 1.5, y - 7 - Dist * 1.5, Dist * 3, Dist * 3, False)
												AAText(x - width / 2 + 10, y - height / 2 + 30 + (20*SCPs_found), "SCP-049")
												SCPs_found% = SCPs_found% + 1
											EndIf
										EndIf
										Exit
									EndIf
								Next
								If PlayerRoom\RoomTemplate\Name = "coffin" Then
									If CoffinDistance < 8.0 Then
										Dist = Rnd(4.0, 8.0)
										Color 100, 0, 0
										Oval(x - Dist * 1.5, y - 7 - Dist * 1.5, Dist * 3, Dist * 3, False)
										AAText(x - width / 2 + 10, y - height / 2 + 30 + (20*SCPs_found), "SCP-895")
									EndIf
								EndIf
							EndIf
							
							Color (30,30,30)
							If SelectedItem\itemtemplate\name = "S-NAV Navigator" Then Color(100, 0, 0)
							If SelectedItem\state <= 100 Then
								xtemp = x - width/2 + 196
								ytemp = y - height/2 + 10
								Rect xtemp,ytemp,80,20,False
								
								For i = 1 To Ceil(SelectedItem\state / 10.0)
									DrawImage NavImages[4],xtemp+i*8-6,ytemp+4
								Next
								AASetFont Font3
							EndIf
						EndIf
					EndIf
					;[End Block]
				Case "scp1499","super1499"
					;[Block]
					If WearingHazmat>0
						Msg = "You are not able to wear SCP-1499 and a hazmat suit at the same time."
						MsgTimer = 70 * 5
						SelectedItem=Null
						Return
					EndIf
					
					CurrSpeed = CurveValue(0, CurrSpeed, 5.0)
					
					DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
					
					width% = 300
					height% = 20
					x% = GraphicWidth / 2 - width / 2
					y% = GraphicHeight / 2 + 80
					Rect(x, y, width+4, height, False)
					For  i% = 1 To Int((width - 2) * (SelectedItem\state / 100.0) / 10)
						DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
					Next
					
					SelectedItem\state = Min(SelectedItem\state+(FPSfactor),100)
					
					If SelectedItem\state=100 Then
						If Wearing1499>0 Then
							Wearing1499 = False
							If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
						Else
							If SelectedItem\itemtemplate\tempname="scp1499" Then
								Wearing1499 = 1
							Else
								Wearing1499 = 2
							EndIf
							If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
							GiveAchievement(Achv1499)
							If WearingNightVision Then CameraFogFar = StoredCameraFogFar
							WearingGasMask = 0
							WearingNightVision = 0
							For r.Rooms = Each Rooms
								If r\RoomTemplate\Name = "dimension1499" Then
									BlinkTimer = -1
									NTF_1499PrevRoom = PlayerRoom
									NTF_1499PrevX# = EntityX(Collider)
									NTF_1499PrevY# = EntityY(Collider)
									NTF_1499PrevZ# = EntityZ(Collider)
									
									If NTF_1499X# = 0.0 And NTF_1499Y# = 0.0 And NTF_1499Z# = 0.0 Then
										PositionEntity (Collider, r\x+6086.0*RoomScale, r\y+304.0*RoomScale, r\z+2292.5*RoomScale)
										RotateEntity Collider,0,90,0,True
									Else
										PositionEntity (Collider, NTF_1499X#, NTF_1499Y#+0.05, NTF_1499Z#)
									EndIf
									ResetEntity(Collider)
									UpdateDoors()
									UpdateRooms()
									For it.Items = Each Items
										it\disttimer = 0
									Next
									PlayerRoom = r
									PlaySound_Strict (LoadTempSound("SFX\SCP\1499\Enter.ogg"))
									NTF_1499X# = 0.0
									NTF_1499Y# = 0.0
									NTF_1499Z# = 0.0
									If Curr096<>Null Then
										If Curr096\SoundChn<>0 Then
											SetStreamVolume_Strict(Curr096\SoundChn,0.0)
										EndIf
									EndIf
									For e.Events = Each Events
										If e\EventName = "dimension1499" Then
											If EntityDistanceSquared(e\room\obj,Collider)>PowTwo(8300.0*RoomScale) Then
												If e\EventState2 < 5 Then
													e\EventState2 = e\EventState2 + 1
												EndIf
											EndIf
											Exit
										EndIf
									Next
									Exit
								EndIf
							Next
						EndIf
						SelectedItem\state=0
						SelectedItem = Null
					EndIf
					;[End Block]
				Case "badge"
					;[Block]
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					DrawImage(SelectedItem\itemtemplate\img, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\img) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\img) / 2)
					
					If SelectedItem\state = 0 Then
						PlaySound_Strict LoadTempSound("SFX\SCP\1162\NostalgiaCancer"+Rand(6,10)+".ogg")
						Select SelectedItem\itemtemplate\name
							Case "Old Badge"
								Msg = Chr(34)+"Huh? This guy looks just like me!"+Chr(34)
								MsgTimer = 70*10
						End Select
						
						SelectedItem\state = 1
					EndIf
					;[End Block]
				Case "key"
					;[Block]
					If SelectedItem\state = 0 Then
						PlaySound_Strict LoadTempSound("SFX\SCP\1162\NostalgiaCancer"+Rand(6,10)+".ogg")
						
						Msg = Chr(34)+"Isn't this the key to that old shack? The one where I... No, it can't be."+Chr(34)
						MsgTimer = 70*10						
					EndIf
					
					SelectedItem\state = 1
					SelectedItem = Null
					;[End Block]
				Case "oldpaper"
					;[Block]
					If SelectedItem\itemtemplate\img = 0 Then
						SelectedItem\itemtemplate\img = LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
						SelectedItem\itemtemplate\img = ResizeImage2(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
						
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					DrawImage(SelectedItem\itemtemplate\img, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\img) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\img) / 2)
					
					If SelectedItem\state = 0
						Select SelectedItem\itemtemplate\name
							Case "Disciplinary Hearing DH-S-4137-17092"
								BlurTimer = 1000
								
								Msg = Chr(34)+"Why does this seem so familiar?"+Chr(34)
								MsgTimer = 70*10
								PlaySound_Strict LoadTempSound("SFX\SCP\1162\NostalgiaCancer"+Rand(6,10)+".ogg")
								SelectedItem\state = 1
						End Select
					EndIf
					;[End Block]
				Case "coin"
					;[Block]
					If SelectedItem\state = 0
						PlaySound_Strict LoadTempSound("SFX\SCP\1162\NostalgiaCancer"+Rand(1,5)+".ogg")
						SelectedItem\state = 1
					EndIf
					
					DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
					;[End Block]
				Case "scp427"
					;[Block]
					If I_427\Using=1 Then
						Msg = "You closed the locket."
						I_427\Using = False
					Else
						GiveAchievement(Achv427)
						Msg = "You opened the locket."
						I_427\Using = True
					EndIf
					MsgTimer = 70 * 5
					SelectedItem = Null
					;[End Block]
				Case "pill"
					;[Block]
					If CanUseItem(False, False, True)
						Msg = "You swallowed the pill."
						MsgTimer = 70*7
						
						RemoveItem(SelectedItem)
						SelectedItem = Null
					EndIf	
					;[End Block]
				Case "scp500death"
					;[Block]
					If CanUseItem(False, False, True)
						Msg = "You swallowed the pill."
						MsgTimer = 70*7
						
						If I_427\Timer < 70*360 Then
							I_427\Timer = 70*360
						EndIf
						
						RemoveItem(SelectedItem)
						SelectedItem = Null
					EndIf
					;[End Block]
				Case "keyjanitor", "keyscientist", "keysupervisor", "keyengineer", "keyguard", "keycadet", "keylieutenant", "keycommander", "keyzmanager", "keyfmanager", "keychaos", "keyO5"
					;[Block]
					MaskImage(SelectedItem\itemtemplate\invimg,255,0,255)
					DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
					;[End Block]
				Default
					;[Block]
					;check if the item is an inventory-type object
					If SelectedItem\invSlots>0 Then
						DoubleClick = 0
						MouseHit1 = 0
						MouseDown1 = 0
						LastMouseHit1 = 0
						OtherOpen = SelectedItem
						SelectedItem = Null
					EndIf
					;[End Block]
			End Select
			
			If SelectedItem <> Null Then
				If SelectedItem\itemtemplate\img <> 0
					Local IN$ = SelectedItem\itemtemplate\tempname
					If IN$ = "paper" Lor IN$ = "badge" Lor IN$ = "oldpaper" Lor IN$ = "ticket" Then
						For a_it.Items = Each Items
							If a_it <> SelectedItem
								Local IN2$ = a_it\itemtemplate\tempname
								If IN2$ = "paper" Lor IN2$ = "badge" Lor IN2$ = "oldpaper" Lor IN2$ = "ticket" Then
									If a_it\itemtemplate\img<>0
										If a_it\itemtemplate\img <> SelectedItem\itemtemplate\img
											FreeImage(a_it\itemtemplate\img)
											a_it\itemtemplate\img = 0
										EndIf
									EndIf
								EndIf
							EndIf
						Next
					EndIf
				EndIf			
			EndIf
			
			If MouseHit2 Then
				EntityAlpha Dark, 0.0
				
				IN$ = SelectedItem\itemtemplate\tempname
				If IN$ = "scp1025" Then
					If SelectedItem\itemtemplate\img<>0 Then FreeImage(SelectedItem\itemtemplate\img)
					SelectedItem\itemtemplate\img=0
				ElseIf IN$ = "firstaid" Lor IN$="finefirstaid" Lor IN$="firstaid2" Then
					SelectedItem\state = 0
				ElseIf IN$ = "vest" Lor IN$="finevest"
					SelectedItem\state = 0
					If (Not WearingVest)
						DropItem(SelectedItem,False)
					EndIf
				ElseIf IN$="hazmatsuit" Lor IN$="hazmatsuit2" Lor IN$="hazmatsuit3"
					SelectedItem\state = 0
					If (Not WearingHazmat)
						DropItem(SelectedItem,False)
					EndIf
				ElseIf IN$="scp1499" Lor IN$="super1499"
					SelectedItem\state = 0
				EndIf
				
				If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX[SelectedItem\itemtemplate\sound])
				SelectedItem = Null
			EndIf
		EndIf		
	EndIf
	
	If SelectedItem = Null Then
		For i = 0 To 6
			If RadioCHN[i] <> 0 Then 
				If ChannelPlaying(RadioCHN[i]) Then PauseChannel(RadioCHN[i])
			EndIf
		Next
	EndIf
	
	For it.Items = Each Items
		If it<>SelectedItem
			Select it\itemtemplate\tempname
				Case "firstaid","finefirstaid","firstaid2","vest","finevest","hazmatsuit","hazmatsuit2","hazmatsuit3","scp1499","super1499"
					it\state = 0
			End Select
		EndIf
	Next
	
	If PrevInvOpen And (Not InvOpen) Then MoveMouse viewport_center_x, viewport_center_y
	
	CatchErrors("DrawGUI")
End Function

Function DrawMenu()
	CatchErrors("Uncaught (DrawMenu)")
	
	Local x%, y%, width%, height%
	
	If (Not InFocus()) Then ;Game is out of focus -> pause the game
		If (Not Using294) Then
			MenuOpen = True
			PauseSounds()
		EndIf
		Delay 1000 ;Reduce the CPU take while game is not in focus
	EndIf
	
	If MenuOpen Then
		If PlayerRoom\RoomTemplate\Name$ <> "exit1" And PlayerRoom\RoomTemplate\Name$ <> "gatea"
			If StopHidingTimer = 0 Then
				If EntityDistanceSquared(Curr173\Collider, Collider)<16.0 Lor EntityDistanceSquared(Curr106\Collider, Collider)<16.0 Then 
					StopHidingTimer = 1
				EndIf	
			ElseIf StopHidingTimer < 40
				If KillTimer >= 0 Then 
					StopHidingTimer = StopHidingTimer+FPSfactor
					
					If StopHidingTimer => 40 Then
						PlaySound_Strict(HorrorSFX[15])
						Msg = "STOP HIDING"
						MsgTimer = 6*70
						MenuOpen = False
						Return
					EndIf
				EndIf
			EndIf
		EndIf
		
		InvOpen = False
		
		width = ImageWidth(PauseMenuIMG)
		height = ImageHeight(PauseMenuIMG)
		x = GraphicWidth / 2 - width / 2
		y = GraphicHeight / 2 - height / 2
		
		DrawImage PauseMenuIMG, x, y
		
		Color(255, 255, 255)
		
		x = x+132*MenuScale
		y = y+122*MenuScale	
		
		If (Not MouseDown1)
			OnSliderID = 0
		EndIf
		
		If AchievementsMenu > 0 Then
			AASetFont Font2
			AAText(x, y-(122-45)*MenuScale, "ACHIEVEMENTS",False,True)
			AASetFont Font1
		ElseIf OptionsMenu > 0 Then
			AASetFont Font2
			AAText(x, y-(122-45)*MenuScale, "OPTIONS",False,True)
			AASetFont Font1
		ElseIf QuitMSG > 0 Then
			AASetFont Font2
			AAText(x, y-(122-45)*MenuScale, "QUIT?",False,True)
			AASetFont Font1
		ElseIf KillTimer >= 0 Then
			AASetFont Font2
			AAText(x, y-(122-45)*MenuScale, "PAUSED",False,True)
			AASetFont Font1
		Else
			AASetFont Font2
			AAText(x, y-(122-45)*MenuScale, "YOU DIED",False,True)
			AASetFont Font1
		EndIf		
		
		Local AchvXIMG% = (x + (22*MenuScale))
		Local scale# = GraphicHeight/768.0
		Local SeparationConst% = 76*scale
		Local imgsize% = 64
		
		If AchievementsMenu <= 0 And OptionsMenu <= 0 And QuitMSG <= 0
			AASetFont Font1
			AAText x, y, "Difficulty: "+SelectedDifficulty\name
			AAText x, y+20*MenuScale, "Save: "+CurrSave
			AAText x, y+40*MenuScale, "Map seed: "+RandomSeed
		ElseIf AchievementsMenu <= 0 And OptionsMenu > 0 And QuitMSG <= 0 And KillTimer >= 0
			If DrawButton(x + 101 * MenuScale, y + 390 * MenuScale, 230 * MenuScale, 60 * MenuScale, "Back") Then
				AchievementsMenu = 0
				OptionsMenu = 0
				QuitMSG = 0
				MouseHit1 = False
				SaveOptionsINI()
				
				AntiAlias Opt_AntiAlias
				TextureLodBias TextureFloat#
			EndIf
			
			Color 0,255,0
			If OptionsMenu = 1
				Rect(x-10*MenuScale,y-5*MenuScale,110*MenuScale,40*MenuScale,True)
			ElseIf OptionsMenu = 2
				Rect(x+100*MenuScale,y-5*MenuScale,110*MenuScale,40*MenuScale,True)
			ElseIf OptionsMenu = 3
				Rect(x+210*MenuScale,y-5*MenuScale,110*MenuScale,40*MenuScale,True)
			ElseIf OptionsMenu = 4
				Rect(x+320*MenuScale,y-5*MenuScale,110*MenuScale,40*MenuScale,True)
			EndIf
			
			If DrawButton(x-5*MenuScale,y,100*MenuScale,30*MenuScale,"GRAPHICS",False) Then OptionsMenu = 1
			If DrawButton(x+105*MenuScale,y,100*MenuScale,30*MenuScale,"AUDIO",False) Then OptionsMenu = 2
			If DrawButton(x+215*MenuScale,y,100*MenuScale,30*MenuScale,"CONTROLS",False) Then OptionsMenu = 3
			If DrawButton(x+325*MenuScale,y,100*MenuScale,30*MenuScale,"ADVANCED",False) Then OptionsMenu = 4
			
			Local tx# = (GraphicWidth/2)+(width/2)
			Local ty# = y
			Local tw# = 400*MenuScale
			Local th# = 150*MenuScale
			
			Color 255,255,255
			Select OptionsMenu
				Case 1 ;Graphics
					AASetFont Font1
					;[Block]
					y=y+50*MenuScale
					
					Color 100,100,100
					AAText(x, y, "Enable bump mapping:")	
					BumpEnabled = DrawTick(x + 270 * MenuScale, y + MenuScale, BumpEnabled, True)
					If MouseOn(x + 270 * MenuScale, y + MenuScale, 20*MenuScale,20*MenuScale) And OnSliderID=0
						DrawOptionsTooltip(tx,ty,tw,th,"bump")
					EndIf
					
					y=y+30*MenuScale
					
					Color 255,255,255
					AAText(x, y, "VSync:")
					Vsync% = DrawTick(x + 270 * MenuScale, y + MenuScale, Vsync%)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale) And OnSliderID=0
						DrawOptionsTooltip(tx,ty,tw,th,"vsync")
					EndIf
					
					y=y+30*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Anti-aliasing:")
					Opt_AntiAlias = DrawTick(x + 270 * MenuScale, y + MenuScale, Opt_AntiAlias%)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale) And OnSliderID=0
						DrawOptionsTooltip(tx,ty,tw,th,"antialias")
					EndIf
					
					y=y+30*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Enable room lights:")
					EnableRoomLights = DrawTick(x + 270 * MenuScale, y + MenuScale, EnableRoomLights)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale) And OnSliderID=0
						DrawOptionsTooltip(tx,ty,tw,th,"roomlights")
					EndIf
					
					y=y+30*MenuScale
					
					ScreenGamma = (SlideBar(x + 270*MenuScale, y+6*MenuScale, 100*MenuScale, ScreenGamma*50.0)/50.0)
					Color 255,255,255
					AAText(x, y, "Screen gamma")
					If MouseOn(x+270*MenuScale,y+6*MenuScale,100*MenuScale+14,20) And OnSliderID=0
						DrawOptionsTooltip(tx,ty,tw,th,"gamma",ScreenGamma)
					EndIf
					
					y=y+50*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Particle amount:")
					ParticleAmount = Slider3(x+270*MenuScale,y+6*MenuScale,100*MenuScale,ParticleAmount,2,"MINIMAL","REDUCED","FULL")
					If (MouseOn(x + 270 * MenuScale, y-6*MenuScale, 100*MenuScale+14, 20) And OnSliderID=0) Lor OnSliderID=2
						DrawOptionsTooltip(tx,ty,tw,th,"particleamount",ParticleAmount)
					EndIf
					
					y=y+50*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Texture LOD Bias:")
					TextureDetails = Slider5(x+270*MenuScale,y+6*MenuScale,100*MenuScale,TextureDetails,3,"0.8","0.4","0.0","-0.4","-0.8")
					Select TextureDetails%
						Case 0
							TextureFloat# = 0.8
						Case 1
							TextureFloat# = 0.4
						Case 2
							TextureFloat# = 0.0
						Case 3
							TextureFloat# = -0.4
						Case 4
							TextureFloat# = -0.8
					End Select
					TextureLodBias TextureFloat
					If (MouseOn(x+270*MenuScale,y-6*MenuScale,100*MenuScale+14,20) And OnSliderID=0) Lor OnSliderID=3
						DrawOptionsTooltip(tx,ty,tw,th+100*MenuScale,"texquality")
					EndIf
					
					y=y+50*MenuScale
					Color 100,100,100
					AAText(x, y, "Save textures in the VRAM:")	
					EnableVRam = DrawTick(x + 270 * MenuScale, y + MenuScale, EnableVRam, True)
					If MouseOn(x + 270 * MenuScale, y + MenuScale, 20*MenuScale,20*MenuScale) And OnSliderID=0
						DrawOptionsTooltip(tx,ty,tw,th,"vram")
					EndIf
					;[End Block]
				Case 2 ;Audio
					AASetFont Font1
					;[Block]
					y = y + 50*MenuScale
					
					MusicVolume = (SlideBar(x + 250*MenuScale, y-4*MenuScale, 100*MenuScale, MusicVolume*100.0)/100.0)
					Color 255,255,255
					AAText(x, y, "Music volume:")
					If MouseOn(x+250*MenuScale,y-4*MenuScale,100*MenuScale+14,20)
						DrawOptionsTooltip(tx,ty,tw,th,"musicvol",MusicVolume)
					EndIf
					
					y = y + 30*MenuScale
					
					PrevSFXVolume = (SlideBar(x + 250*MenuScale, y-4*MenuScale, 100*MenuScale, SFXVolume*100.0)/100.0)
					If (Not DeafPlayer) Then SFXVolume# = PrevSFXVolume#
					Color 255,255,255
					AAText(x, y, "Sound volume:")
					If MouseOn(x+250*MenuScale,y-4*MenuScale,100*MenuScale+14,20)
						DrawOptionsTooltip(tx,ty,tw,th,"soundvol",PrevSFXVolume)
					EndIf
					
					y = y + 30*MenuScale
					
					Color 100,100,100
					AAText x, y, "Sound auto-release:"
					EnableSFXRelease = DrawTick(x + 270 * MenuScale, y + MenuScale, EnableSFXRelease,True)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th+220*MenuScale,"sfxautorelease")
					EndIf
					
					y = y + 30*MenuScale
					
					Color 100,100,100
					AAText x, y, "Enable user tracks:"
					EnableUserTracks = DrawTick(x + 270 * MenuScale, y + MenuScale, EnableUserTracks,True)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"usertrack")
					EndIf
					
					If EnableUserTracks
						y = y + 30 * MenuScale
						Color 255,255,255
						AAText x, y, "User track mode:"
						UserTrackMode = DrawTick(x + 270 * MenuScale, y + MenuScale, UserTrackMode)
						If UserTrackMode
							AAText x, y + 20 * MenuScale, "Repeat"
						Else
							AAText x, y + 20 * MenuScale, "Random"
						EndIf
						If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
							DrawOptionsTooltip(tx,ty,tw,th,"usertrackmode")
						EndIf
					EndIf
					;[End Block]
				Case 3 ;Controls
					AASetFont Font1
					;[Block]
					y = y + 50*MenuScale
					
					MouseSens = (SlideBar(x + 270*MenuScale, y-4*MenuScale, 100*MenuScale, (MouseSens+0.5)*100.0)/100.0)-0.5
					Color(255, 255, 255)
					AAText(x, y, "Mouse sensitivity:")
					If MouseOn(x+270*MenuScale,y-4*MenuScale,100*MenuScale+14,20)
						DrawOptionsTooltip(tx,ty,tw,th,"mousesensitivity",MouseSens)
					EndIf
					
					y = y + 30*MenuScale
					
					Color(255, 255, 255)
					AAText(x, y, "Invert mouse Y-axis:")
					InvertMouse = DrawTick(x + 270 * MenuScale, y + MenuScale, InvertMouse)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"mouseinvert")
					EndIf
					
					y = y + 40*MenuScale
					
					MouseSmooth = (SlideBar(x + 270*MenuScale, y-4*MenuScale, 100*MenuScale, (MouseSmooth)*50.0)/50.0)
					Color(255, 255, 255)
					AAText(x, y, "Mouse smoothing:")
					If MouseOn(x+270*MenuScale,y-4*MenuScale,100*MenuScale+14,20)
						DrawOptionsTooltip(tx,ty,tw,th,"mousesmoothing",MouseSmooth)
					EndIf
					
					Color(255, 255, 255)
					
					y = y + 30*MenuScale
					AAText(x, y, "Control configuration:")
					y = y + 10*MenuScale
					
					AAText(x, y + 20 * MenuScale, "Move Forward")
					InputBox(x + 200 * MenuScale, y + 20 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_UP,210)],5)		
					AAText(x, y + 40 * MenuScale, "Strafe Left")
					InputBox(x + 200 * MenuScale, y + 40 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_LEFT,210)],3)	
					AAText(x, y + 60 * MenuScale, "Move Backward")
					InputBox(x + 200 * MenuScale, y + 60 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_DOWN,210)],6)				
					AAText(x, y + 80 * MenuScale, "Strafe Right")
					InputBox(x + 200 * MenuScale, y + 80 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_RIGHT,210)],4)
					
					AAText(x, y + 100 * MenuScale, "Manual Blink")
					InputBox(x + 200 * MenuScale, y + 100 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_BLINK,210)],7)				
					AAText(x, y + 120 * MenuScale, "Sprint")
					InputBox(x + 200 * MenuScale, y + 120 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_SPRINT,210)],8)
					AAText(x, y + 140 * MenuScale, "Open/Close Inventory")
					InputBox(x + 200 * MenuScale, y + 140 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_INV,210)],9)
					AAText(x, y + 160 * MenuScale, "Crouch")
					InputBox(x + 200 * MenuScale, y + 160 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_CROUCH,210)],10)
					AAText(x, y + 180 * MenuScale, "Quick Save")
					InputBox(x + 200 * MenuScale, y + 180 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_SAVE,210)],11)	
					AAText(x, y + 200 * MenuScale, "Open/Close Console")
					InputBox(x + 200 * MenuScale, y + 200 * MenuScale,100*MenuScale,20*MenuScale,KeyName[Min(KEY_CONSOLE,210)],12)
					
					If MouseOn(x,y,300*MenuScale,220*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"controls")
					EndIf
					
					For i = 0 To 227
						If KeyHit(i) Then key = i : Exit
					Next
					If key <> 0 Then
						Select SelectedInputBox
							Case 3
								KEY_LEFT = key
							Case 4
								KEY_RIGHT = key
							Case 5
								KEY_UP = key
							Case 6
								KEY_DOWN = key
							Case 7
								KEY_BLINK = key
							Case 8
								KEY_SPRINT = key
							Case 9
								KEY_INV = key
							Case 10
								KEY_CROUCH = key
							Case 11
								KEY_SAVE = key
							Case 12
								KEY_CONSOLE = key
						End Select
						SelectedInputBox = 0
					EndIf
					;[End Block]
				Case 4 ;Advanced
					AASetFont Font1
					;[Block]
					y = y + 50*MenuScale
					
					Color 255,255,255				
					AAText(x, y, "Show HUD:")	
					HUDenabled = DrawTick(x + 270 * MenuScale, y + MenuScale, HUDenabled)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"hud")
					EndIf
					
					y = y + 30*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Enable console:")
					CanOpenConsole = DrawTick(x +270 * MenuScale, y + MenuScale, CanOpenConsole)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"consoleenable")
					EndIf
					
					y = y + 30*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Open console on error:")
					ConsoleOpening = DrawTick(x + 270 * MenuScale, y + MenuScale, ConsoleOpening)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"consoleerror")
					EndIf
					
					y = y + 50*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Achievement popups:")
					AchvMSGenabled% = DrawTick(x + 270 * MenuScale, y, AchvMSGenabled%)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"achpopup")
					EndIf
					
					y = y + 50*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Show FPS:")
					ShowFPS% = DrawTick(x + 270 * MenuScale, y, ShowFPS%)
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"showfps")
					EndIf
					
					y = y + 30*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Framelimit:")
					
					Color 255,255,255
					If DrawTick(x + 270 * MenuScale, y, CurrFrameLimit > 0.0) Then
						CurrFrameLimit# = (SlideBar(x + 150*MenuScale, y+30*MenuScale, 100*MenuScale, CurrFrameLimit#*99.0)/99.0)
						CurrFrameLimit# = Max(CurrFrameLimit, 0.01)
						Framelimit% = 19+(CurrFrameLimit*100.0)
						Color 255,255,0
						AAText(x + 5 * MenuScale, y + 25 * MenuScale, Framelimit%+" FPS")
					Else
						CurrFrameLimit# = 0.0
						Framelimit = 0
					EndIf
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"framelimit",Framelimit)
					EndIf
					If MouseOn(x+150*MenuScale,y+30*MenuScale,100*MenuScale+14,20)
						DrawOptionsTooltip(tx,ty,tw,th,"framelimit",Framelimit)
					EndIf
					
					y = y + 80*MenuScale
					
					Color 255,255,255
					AAText(x, y, "Antialiased text:")
					AATextEnable% = DrawTick(x + 270 * MenuScale, y + MenuScale, AATextEnable%)
					If AATextEnable_Prev% <> AATextEnable
						For font.AAFont = Each AAFont
							FreeFont font\lowResFont%
							If (Not AATextEnable)
								FreeTexture font\texture
								FreeImage font\backup
							EndIf
							Delete font
						Next
						If (Not AATextEnable) Then
							FreeEntity AATextCam
						EndIf
						InitAAFont()
						Font1% = AALoadFont("GFX\font\cour\Courier New.ttf", 16)
						Font2% = AALoadFont("GFX\font\cour\Courier New.ttf", 52)
						Font3% = AALoadFont("GFX\font\DS-DIGI\DS-Digital.ttf", 20)
						Font4% = AALoadFont("GFX\font\DS-DIGI\DS-Digital.ttf", 60)
						Font5% = AALoadFont("GFX\font\Journal\Journal.ttf", 58)
						ConsoleFont% = AALoadFont("GFX\font\Andale\Andale Mono.ttf", 16)
						AATextEnable_Prev% = AATextEnable
					EndIf
					If MouseOn(x+270*MenuScale,y+MenuScale,20*MenuScale,20*MenuScale)
						DrawOptionsTooltip(tx,ty,tw,th,"antialiastext")
					EndIf
					;[End Block]
			End Select
		ElseIf AchievementsMenu <= 0 And OptionsMenu <= 0 And QuitMSG > 0 And KillTimer >= 0
			Local QuitButton% = 60 
			
			If SelectedDifficulty\saveType = SAVEONQUIT Lor SelectedDifficulty\saveType = SAVEANYWHERE Then
				Local RN$ = PlayerRoom\RoomTemplate\Name$
				Local AbleToSave% = True
				
				If RN$ = "173" Lor RN$ = "exit1" Lor RN$ = "gatea" Then AbleToSave = False
				If (Not CanSave) Then AbleToSave = False
				If AbleToSave
					QuitButton = 140
					If DrawButton(x, y + 60*MenuScale, 390*MenuScale, 60*MenuScale, "Save & Quit") Then
						DropSpeed = 0
						SaveGame(SavePath + CurrSave + "\")
						NullGame()
						MenuOpen = False
						MainMenuOpen = True
						MainMenuTab = 0
						CurrSave = ""
						FlushKeys()
					EndIf
				EndIf
			EndIf
			
			If DrawButton(x, y + QuitButton*MenuScale, 390*MenuScale, 60*MenuScale, "Quit") Then
				NullGame()
				MenuOpen = False
				MainMenuOpen = True
				MainMenuTab = 0
				CurrSave = ""
				FlushKeys()
			EndIf
			
			If DrawButton(x+101*MenuScale, y + 344*MenuScale, 230*MenuScale, 60*MenuScale, "Back") Then
				AchievementsMenu = 0
				OptionsMenu = 0
				QuitMSG = 0
				MouseHit1 = False
			EndIf
		Else
			If DrawButton(x+101*MenuScale, y + 344*MenuScale, 230*MenuScale, 60*MenuScale, "Back") Then
				AchievementsMenu = 0
				OptionsMenu = 0
				QuitMSG = 0
				MouseHit1 = False
			EndIf
			
			If AchievementsMenu>0 Then
				If AchievementsMenu <= Floor(Float(MAXACHIEVEMENTS-1)/12.0) Then 
					If DrawButton(x+341*MenuScale, y + 344*MenuScale, 50*MenuScale, 60*MenuScale, ">") Then
						AchievementsMenu = AchievementsMenu+1
					EndIf
				EndIf
				If AchievementsMenu > 1 Then
					If DrawButton(x+41*MenuScale, y + 344*MenuScale, 50*MenuScale, 60*MenuScale, "<") Then
						AchievementsMenu = AchievementsMenu-1
					EndIf
				EndIf
				
				For i=0 To 11
					If i+((AchievementsMenu-1)*12)<MAXACHIEVEMENTS Then
						DrawAchvIMG(AchvXIMG,y+((i/4)*120*MenuScale),i+((AchievementsMenu-1)*12))
					Else
						Exit
					EndIf
				Next
				
				For i=0 To 11
					If i+((AchievementsMenu-1)*12)<MAXACHIEVEMENTS Then
						If MouseOn(AchvXIMG+((i Mod 4)*SeparationConst),y+((i/4)*120*MenuScale),64*scale,64*scale) Then
							AchievementTooltip(i+((AchievementsMenu-1)*12))
							Exit
						EndIf
					Else
						Exit
					EndIf
				Next
			EndIf
		EndIf
		
		y = y+10
		
		If AchievementsMenu<=0 And OptionsMenu<=0 And QuitMSG<=0 Then
			If KillTimer >= 0 Then	
				y = y+ 72*MenuScale
				
				If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Resume", True, True) Then
					MenuOpen = False
					ResumeSounds()
					MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
				EndIf
				
				y = y + 75*MenuScale
				If (Not SelectedDifficulty\permaDeath) Then
					If GameSaved Then
						If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Load Game") Then
							DrawLoading(0)
							
							MenuOpen = False
							LoadGameQuick(SavePath + CurrSave + "\")
							
							MoveMouse viewport_center_x,viewport_center_y
							AASetFont Font1
							HidePointer ()
							
							FlushKeys()
							FlushMouse()
							Playable=True
							
							UpdateRooms()
							
							For r.Rooms = Each Rooms
								x = Abs(EntityX(Collider) - EntityX(r\obj))
								z = Abs(EntityZ(Collider) - EntityZ(r\obj))
								
								If x < 12.0 And z < 12.0 Then
									MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)) = Max(MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)), 1)
									If x < 4.0 And z < 4.0 Then
										If Abs(EntityY(Collider) - EntityY(r\obj)) < 1.5 Then PlayerRoom = r
										MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)) = 1
									EndIf
								EndIf
							Next
							
							DrawLoading(100)
							
							DropSpeed=0
							
							UpdateWorld 0.0
							
							PrevTime = MilliSecs()
							FPSfactor = 0
							
							ResetInput()
						EndIf
					Else
						DrawFrame(x,y,390*MenuScale, 60*MenuScale)
						Color (100, 100, 100)
						AASetFont Font2
						AAText(x + (390*MenuScale) / 2, y + (60*MenuScale) / 2, "Load Game", True, True)
					EndIf
					y = y + 75*MenuScale
			EndIf
			If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Achievements") Then AchievementsMenu = 1
				y = y + 75*MenuScale
				If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Options") Then OptionsMenu = 1
				y = y + 75*MenuScale
			Else
				y = y+104*MenuScale
				If GameSaved And (Not SelectedDifficulty\permaDeath) Then
					If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Load Game") Then
						DrawLoading(0)
						
						MenuOpen = False
						LoadGameQuick(SavePath + CurrSave + "\")
						
						MoveMouse viewport_center_x,viewport_center_y
						AASetFont Font1
						HidePointer ()
						
						FlushKeys()
						FlushMouse()
						Playable=True
						
						UpdateRooms()
						
						For r.Rooms = Each Rooms
							x = Abs(EntityX(Collider) - EntityX(r\obj))
							z = Abs(EntityZ(Collider) - EntityZ(r\obj))
							
							If x < 12.0 And z < 12.0 Then
								MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)) = Max(MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)), 1)
								If x < 4.0 And z < 4.0 Then
									If Abs(EntityY(Collider) - EntityY(r\obj)) < 1.5 Then PlayerRoom = r
									MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)) = 1
								EndIf
							EndIf
						Next
						
						DrawLoading(100)
						
						DropSpeed=0
						
						UpdateWorld 0.0
						
						PrevTime = MilliSecs()
						FPSfactor = 0
						
						ResetInput()
					EndIf
				Else
					DrawButton(x, y, 390*MenuScale, 60*MenuScale, "")
					Color 50,50,50
					AAText(x + 185*MenuScale, y + 30*MenuScale, "Load Game", True, True)
				EndIf
				If DrawButton(x, y + 80*MenuScale, 390*MenuScale, 60*MenuScale, "Quit to Menu") Then
					NullGame()
					MenuOpen = False
					MainMenuOpen = True
					MainMenuTab = 0
					CurrSave = ""
					FlushKeys()
				EndIf
				y= y + 80*MenuScale
			EndIf
			
			If KillTimer >= 0 And (Not MainMenuOpen)
				If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Quit") Then
					QuitMSG = 1
				EndIf
			EndIf
			
			AASetFont Font1
			If KillTimer < 0 Then RowText(DeathMSG$, x, y + 80*MenuScale, 390*MenuScale, 600*MenuScale)
		EndIf
		If Fullscreen Then DrawImage CursorIMG, ScaledMouseX(),ScaledMouseY()
	EndIf
	
	AASetFont Font1
	
	CatchErrors("DrawMenu")
End Function

Function LoadEntities()
	CatchErrors("Uncaught (LoadEntities)")
	
	loadingasset = "Loading entities"
	DrawLoading(0)
	Local i%
	
	For i=0 To 9
		TempSounds[i]=0
	Next
	
	PauseMenuIMG% = LoadImage_Strict("GFX\menu\pausemenu.jpg")
	MaskImage PauseMenuIMG, 255,255,0
	ScaleImage PauseMenuIMG,MenuScale,MenuScale
	
	SprintIcon% = LoadImage_Strict("GFX\sprinticon.png")
	BlinkIcon% = LoadImage_Strict("GFX\blinkicon.png")
	CrouchIcon% = LoadImage_Strict("GFX\sneakicon.png")
	HandIcon% = LoadImage_Strict("GFX\handsymbol.png")
	HandIcon2% = LoadImage_Strict("GFX\handsymbol2.png")

	StaminaMeterIMG% = LoadImage_Strict("GFX\staminameter.jpg")

	KeypadHUD =  LoadImage_Strict("GFX\keypadhud.jpg")
	MaskImage(KeypadHUD, 255,0,255)

	Panel294 = LoadImage_Strict("GFX\294panel.jpg")
	MaskImage(Panel294, 255,0,255)
	
	CameraFogNear# = GetINIFloat("options.ini", "options", "camera fog near")
	CameraFogFar# = GetINIFloat("options.ini", "options", "camera fog far")
	StoredCameraFogFar# = CameraFogFar
	
	AmbientLightRoomTex% = CreateTexture(2,2,257)
	TextureBlend AmbientLightRoomTex,5
	SetBuffer(TextureBuffer(AmbientLightRoomTex))
	ClsColor 0,0,0
	Cls
	SetBuffer BackBuffer()
	AmbientLightRoomVal = 0
	
	SoundEmitter = CreatePivot()
	
	Camera = CreateCamera()
	CameraViewport Camera,0,0,GraphicWidth,GraphicHeight
	CameraRange(Camera, 0.05, CameraFogFar)
	CameraFogMode (Camera, 1)
	CameraFogRange (Camera, CameraFogNear, CameraFogFar)
	CameraFogColor (Camera, 0, 0, 0)
	AmbientLight BRIGHTNESS, BRIGHTNESS, BRIGHTNESS
	
	ScreenTexs[0] = CreateTexture(512, 512, 1+256)
	ScreenTexs[1] = CreateTexture(512, 512, 1+256)
	
	CreateBlurImage()
	CameraProjMode ark_blur_cam,0
	
	FogTexture = LoadTexture_Strict("GFX\fog.jpg", 1)
	
	Fog = CreateSprite(ark_blur_cam)
	ScaleSprite(Fog, Max(GraphicWidth / 1240.0, 1.0), Max(GraphicHeight / 960.0 * 0.8, 0.8))
	EntityTexture(Fog, FogTexture)
	EntityBlend (Fog, 2)
	EntityOrder Fog, -1000
	MoveEntity(Fog, 0, 0, 1.0)
	
	GasMaskTexture = LoadTexture_Strict("GFX\GasmaskOverlay.jpg", 1)
	GasMaskOverlay = CreateSprite(ark_blur_cam)
	ScaleSprite(GasMaskOverlay, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityTexture(GasMaskOverlay, GasMaskTexture)
	EntityBlend (GasMaskOverlay, 2)
	EntityFX(GasMaskOverlay, 1)
	EntityOrder GasMaskOverlay, -1003
	MoveEntity(GasMaskOverlay, 0, 0, 1.0)
	HideEntity(GasMaskOverlay)
	
	InfectTexture = LoadTexture_Strict("GFX\InfectOverlay.jpg", 1)
	InfectOverlay = CreateSprite(ark_blur_cam)
	ScaleSprite(InfectOverlay, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityTexture(InfectOverlay, InfectTexture)
	EntityBlend (InfectOverlay, 3)
	EntityFX(InfectOverlay, 1)
	EntityOrder InfectOverlay, -1003
	MoveEntity(InfectOverlay, 0, 0, 1.0)
	HideEntity(InfectOverlay)
	
	NVTexture = LoadTexture_Strict("GFX\NightVisionOverlay.jpg", 1)
	NVOverlay = CreateSprite(ark_blur_cam)
	ScaleSprite(NVOverlay, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityTexture(NVOverlay, NVTexture)
	EntityBlend (NVOverlay, 2)
	EntityFX(NVOverlay, 1)
	EntityOrder NVOverlay, -1003
	MoveEntity(NVOverlay, 0, 0, 1.0)
	HideEntity(NVOverlay)
	NVBlink = CreateSprite(ark_blur_cam)
	ScaleSprite(NVBlink, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityColor(NVBlink,0,0,0)
	EntityFX(NVBlink, 1)
	EntityOrder NVBlink, -1005
	MoveEntity(NVBlink, 0, 0, 1.0)
	HideEntity(NVBlink)
	
	FogNVTexture = LoadTexture_Strict("GFX\fogNV.jpg", 1)
	
	DrawLoading(5)
	
	DarkTexture = CreateTexture(1024, 1024, 1 + 2)
	SetBuffer TextureBuffer(DarkTexture)
	Cls
	SetBuffer BackBuffer()
	
	Dark = CreateSprite(Camera)
	ScaleSprite(Dark, Max(GraphicWidth / 1240.0, 1.0), Max(GraphicHeight / 960.0 * 0.8, 0.8))
	EntityTexture(Dark, DarkTexture)
	EntityBlend (Dark, 1)
	EntityOrder Dark, -1002
	MoveEntity(Dark, 0, 0, 1.0)
	EntityAlpha Dark, 0.0
	
	LightTexture = CreateTexture(1024, 1024, 1 + 2+256)
	SetBuffer TextureBuffer(LightTexture)
	ClsColor 255, 255, 255
	Cls
	ClsColor 0, 0, 0
	SetBuffer BackBuffer()
	
	TeslaTexture = LoadTexture_Strict("GFX\map\tesla.jpg", 1+2)
	
	Light = CreateSprite(Camera)
	ScaleSprite(Light, Max(GraphicWidth / 1240.0, 1.0), Max(GraphicHeight / 960.0 * 0.8, 0.8))
	EntityTexture(Light, LightTexture)
	EntityBlend (Light, 1)
	EntityOrder Light, -1002
	MoveEntity(Light, 0, 0, 1.0)
	HideEntity Light
	
	Collider = CreatePivot()
	EntityRadius Collider, 0.15, 0.30
	EntityPickMode(Collider, 1)
	EntityType Collider, HIT_PLAYER
	
	Head = CreatePivot()
	EntityRadius Head, 0.15
	EntityType Head, HIT_PLAYER
	
	LiquidObj = LoadMesh_Strict("GFX\items\cupliquid.x") ;optimized the cups dispensed by 294
	HideEntity LiquidObj
	
	MTFObj = LoadAnimMesh_Strict("GFX\npcs\MTF2.b3d") ;optimized MTFs
	GuardObj = LoadAnimMesh_Strict("GFX\npcs\guard.b3d") ;optimized Guards
	
	ClassDObj = LoadAnimMesh_Strict("GFX\npcs\classd.b3d") ;optimized Class-D's and scientists/researchers
	ApacheObj = LoadAnimMesh_Strict("GFX\apache.b3d") ;optimized Apaches (helicopters)
	ApacheRotorObj = LoadAnimMesh_Strict("GFX\apacherotor.b3d") ;optimized the Apaches even more
	
	HideEntity MTFObj
	HideEntity GuardObj
	HideEntity ClassDObj
	HideEntity ApacheObj
	HideEntity ApacheRotorObj
	
	;Other NPCs pre-loaded
	NPC049OBJ = LoadAnimMesh_Strict("GFX\npcs\scp-049.b3d")
	HideEntity NPC049OBJ
	NPC0492OBJ = LoadAnimMesh_Strict("GFX\npcs\zombie1.b3d")
	HideEntity NPC0492OBJ
	ClerkOBJ = LoadAnimMesh_Strict("GFX\npcs\clerk.b3d")
	HideEntity ClerkOBJ	
	
	LightSpriteTex[0] = LoadTexture_Strict("GFX\light1.jpg", 1)
	LightSpriteTex[1] = LoadTexture_Strict("GFX\light2.jpg", 1)
	LightSpriteTex[2] = LoadTexture_Strict("GFX\lightsprite.jpg",1)
	
	DrawLoading(10)
	
	DoorOBJ = LoadMesh_Strict("GFX\map\door01.x")
	HideEntity DoorOBJ
	DoorFrameOBJ = LoadMesh_Strict("GFX\map\doorframe.x")
	HideEntity DoorFrameOBJ
	
	HeavyDoorObj[0] = LoadMesh_Strict("GFX\map\heavydoor1.x")
	HideEntity HeavyDoorObj[0]
	HeavyDoorObj[1] = LoadMesh_Strict("GFX\map\heavydoor2.x")
	HideEntity HeavyDoorObj[1]
	
	DoorColl = LoadMesh_Strict("GFX\map\doorcoll.x")
	HideEntity DoorColl
	
	ButtonOBJ = LoadMesh_Strict("GFX\map\Button.x")
	HideEntity ButtonOBJ
	ButtonKeyOBJ = LoadMesh_Strict("GFX\map\ButtonKeycard.x")
	HideEntity ButtonKeyOBJ
	ButtonCodeOBJ = LoadMesh_Strict("GFX\map\ButtonCode.x")
	HideEntity ButtonCodeOBJ	
	ButtonScannerOBJ = LoadMesh_Strict("GFX\map\ButtonScanner.x")
	HideEntity ButtonScannerOBJ	
	
	BigDoorOBJ[0] = LoadMesh_Strict("GFX\map\ContDoorLeft.x")
	HideEntity BigDoorOBJ[0]
	BigDoorOBJ[1] = LoadMesh_Strict("GFX\map\ContDoorRight.x")
	HideEntity BigDoorOBJ[1]
	
	LeverBaseOBJ = LoadMesh_Strict("GFX\map\leverbase.x")
	HideEntity LeverBaseOBJ
	LeverOBJ = LoadMesh_Strict("GFX\map\leverhandle.x")
	HideEntity LeverOBJ
	
	DrawLoading(15)
	loadingasset = "Loading images"
	
	For i = 0 To 5
		GorePics[i] = LoadTexture_Strict("GFX\895pics\pic" + (i + 1) + ".jpg")
	Next
	
	OldAiPics[0] = LoadTexture_Strict("GFX\AIface.jpg")
	OldAiPics[1] = LoadTexture_Strict("GFX\AIface2.jpg")	
	
	DrawLoading(20)
	
	For i = 0 To 6
		DecalTextures[i] = LoadTexture_Strict("GFX\decal" + (i + 1) + ".png", 1 + 2)
	Next
	DecalTextures[7] = LoadTexture_Strict("GFX\items\INVpaperstrips.jpg", 1 + 2)
	For i = 8 To 12
		DecalTextures[i] = LoadTexture_Strict("GFX\decalpd"+(i-7)+".jpg", 1 + 2)	
	Next
	For i = 13 To 14
		DecalTextures[i] = LoadTexture_Strict("GFX\bullethole"+(i-12)+".jpg", 1 + 2)	
	Next	
	For i = 15 To 16
		DecalTextures[i] = LoadTexture_Strict("GFX\blooddrop"+(i-14)+".png", 1 + 2)	
	Next
	DecalTextures[17] = LoadTexture_Strict("GFX\decal8.png", 1 + 2)	
	DecalTextures[18] = LoadTexture_Strict("GFX\decalpd6.dc", 1 + 2)	
	DecalTextures[19] = LoadTexture_Strict("GFX\decal19.png", 1 + 2)
	DecalTextures[20] = LoadTexture_Strict("GFX\decal427.png", 1 + 2)
	
	DrawLoading(25)
	
	Monitor = LoadMesh_Strict("GFX\map\monitor.b3d")
	HideEntity Monitor
	MonitorTexture = LoadTexture_Strict("GFX\monitortexture.jpg")
	
	CamBaseOBJ = LoadMesh_Strict("GFX\map\cambase.x")
	HideEntity(CamBaseOBJ)
	CamOBJ = LoadMesh_Strict("GFX\map\CamHead.b3d")
	HideEntity(CamOBJ)
	
	Monitor2 = LoadMesh_Strict("GFX\map\monitor_checkpoint.b3d")
	HideEntity Monitor2
	Monitor3 = LoadMesh_Strict("GFX\map\monitor_checkpoint.b3d")
	HideEntity Monitor3
	MonitorTexture2 = LoadTexture_Strict("GFX\map\LockdownScreen2.jpg")
	MonitorTexture3 = LoadTexture_Strict("GFX\map\LockdownScreen.jpg")
	MonitorTexture4 = LoadTexture_Strict("GFX\map\LockdownScreen3.jpg")
	MonitorTextureOff = CreateTexture(1,1)
	SetBuffer TextureBuffer(MonitorTextureOff)
	ClsColor 0,0,0
	Cls
	SetBuffer BackBuffer()
	LightConeModel = LoadMesh_Strict("GFX\lightcone.b3d")
	HideEntity LightConeModel
	
	For i = 2 To CountSurfaces(Monitor2)
		sf = GetSurface(Monitor2,i)
		b = GetSurfaceBrush(sf)
		If b<>0 Then
			t1 = GetBrushTexture(b,0)
			If t1<>0 Then
				name$ = StripPath(TextureName(t1))
				If Lower(name) <> "monitortexture.jpg"
					BrushTexture b, MonitorTextureOff, 0, 0
					PaintSurface sf,b
				EndIf
				If name<>"" Then FreeTexture t1
			EndIf
			FreeBrush b
		EndIf
	Next
	For i = 2 To CountSurfaces(Monitor3)
		sf = GetSurface(Monitor3,i)
		b = GetSurfaceBrush(sf)
		If b<>0 Then
			t1 = GetBrushTexture(b,0)
			If t1<>0 Then
				name$ = StripPath(TextureName(t1))
				If Lower(name) <> "monitortexture.jpg"
					BrushTexture b, MonitorTextureOff, 0, 0
					PaintSurface sf,b
				EndIf
				If name<>"" Then FreeTexture t1
			EndIf
			FreeBrush b
		EndIf
	Next
	
	UserTrackMusicAmount% = 0
	If EnableUserTracks Then
		Local dirPath$ = "SFX\Radio\UserTracks\"
		
		If FileType(dirPath)<>2 Then
			CreateDir(dirPath)
		EndIf
		
		Local Dir% = ReadDir("SFX\Radio\UserTracks\")
		
		Repeat
			file$=NextFile(Dir)
			If file$="" Then Exit
			If FileType("SFX\Radio\UserTracks\"+file$) = 1 Then
				test = LoadSound("SFX\Radio\UserTracks\"+file$)
				If test<>0
					UserTrackName[UserTrackMusicAmount%] = file$
					UserTrackMusicAmount% = UserTrackMusicAmount% + 1
				EndIf
				FreeSound test
			EndIf
		Forever
		CloseDir Dir
	EndIf
	If EnableUserTracks Then DebugLog "User Tracks found: "+UserTrackMusicAmount
	
	InitItemTemplates()
	
	ParticleTextures[0] = LoadTexture_Strict("GFX\smoke.png", 1 + 2)
	ParticleTextures[1] = LoadTexture_Strict("GFX\flash.jpg", 1 + 2)
	ParticleTextures[2] = LoadTexture_Strict("GFX\dust.jpg", 1 + 2)
	ParticleTextures[3] = LoadTexture_Strict("GFX\npcs\hg.pt", 1 + 2)
	ParticleTextures[4] = LoadTexture_Strict("GFX\map\sun.jpg", 1 + 2)
	ParticleTextures[5] = LoadTexture_Strict("GFX\bloodsprite.png", 1 + 2)
	ParticleTextures[6] = LoadTexture_Strict("GFX\smoke2.png", 1 + 2)
	ParticleTextures[7] = LoadTexture_Strict("GFX\spark.jpg", 1 + 2)
	ParticleTextures[8] = LoadTexture_Strict("GFX\particle.png", 1 + 2)
	
	SetChunkDataValues()
	
	;NPCtypeD - different models with different textures (loaded using "CopyEntity") - ENDSHN
	For i=0 To MaxDTextures-1
		DTextures[i] = CopyEntity(ClassDObj)
		HideEntity DTextures[i]
	Next
	;Gonzales
	tex = LoadTexture_Strict("GFX\npcs\gonzales.jpg")
	EntityTexture DTextures[0],tex
	FreeTexture tex
	;SCP-970 corpse
	tex = LoadTexture_Strict("GFX\npcs\corpse.jpg")
	EntityTexture DTextures[1],tex
	FreeTexture tex
	;scientist 1
	tex = LoadTexture_Strict("GFX\npcs\scientist.jpg")
	EntityTexture DTextures[2],tex
	FreeTexture tex
	;scientist 2
	tex = LoadTexture_Strict("GFX\npcs\scientist2.jpg")
	EntityTexture DTextures[3],tex
	FreeTexture tex
	;janitor
	tex = LoadTexture_Strict("GFX\npcs\janitor.jpg")
	EntityTexture DTextures[4],tex
	FreeTexture tex
	;106 Victim
	tex = LoadTexture_Strict("GFX\npcs\106victim.jpg")
	EntityTexture DTextures[5],tex
	FreeTexture tex
	;2nd ClassD
	tex = LoadTexture_Strict("GFX\npcs\classd2.jpg")
	EntityTexture DTextures[6],tex
	FreeTexture tex
	;035 victim
	tex = LoadTexture_Strict("GFX\npcs\035victim.jpg")
	EntityTexture DTextures[7],tex
	FreeTexture tex
	
	LoadMaterials("DATA\materials.ini")
	
	OBJTunnel[0]=LoadRMesh("GFX\map\mt1.rmesh",Null)	
	HideEntity OBJTunnel[0]				
	OBJTunnel[1]=LoadRMesh("GFX\map\mt2.rmesh",Null)	
	HideEntity OBJTunnel[1]
	OBJTunnel[2]=LoadRMesh("GFX\map\mt2c.rmesh",Null)	
	HideEntity OBJTunnel[2]				
	OBJTunnel[3]=LoadRMesh("GFX\map\mt3.rmesh",Null)	
	HideEntity OBJTunnel[3]	
	OBJTunnel[4]=LoadRMesh("GFX\map\mt4.rmesh",Null)	
	HideEntity OBJTunnel[4]				
	OBJTunnel[5]=LoadRMesh("GFX\map\mt_elevator.rmesh",Null)
	HideEntity OBJTunnel[5]
	OBJTunnel[6]=LoadRMesh("GFX\map\mt_generator.rmesh",Null)
	HideEntity OBJTunnel[6]
	
	TextureLodBias TextureFloat#
	
	;Devil Particle System
	;ParticleEffect[] numbers:
	;	0 - electric spark
	;	1 - smoke effect
	Local t0
	
	InitParticles(Camera)
	
	;Spark Effect (short)
	ParticleEffect[0] = CreateTemplate()
	SetTemplateEmitterBlend(ParticleEffect[0], 3)
	SetTemplateInterval(ParticleEffect[0], 1)
	SetTemplateParticlesPerInterval(ParticleEffect[0], 6)
	SetTemplateEmitterLifeTime(ParticleEffect[0], 6)
	SetTemplateParticleLifeTime(ParticleEffect[0], 20, 30)
	SetTemplateTexture(ParticleEffect[0], "GFX\Spark.png", 2, 3)
	SetTemplateOffset(ParticleEffect[0], -0.1, 0.1, -0.1, 0.1, -0.1, 0.1)
	SetTemplateVelocity(ParticleEffect[0], -0.0375, 0.0375, -0.0375, 0.0375, -0.0375, 0.0375)
	SetTemplateAlignToFall(ParticleEffect[0], True, 45)
	SetTemplateGravity(ParticleEffect[0], 0.001)
	SetTemplateAlphaVel(ParticleEffect[0], True)
	SetTemplateSize(ParticleEffect[0], 0.03125, 0.0625, 0.7, 1)
	SetTemplateColors(ParticleEffect[0], $0000FF, $6565FF)
	SetTemplateFloor(ParticleEffect[0], 0.0, 0.5)
	
	;Smoke effect (for some vents)
	ParticleEffect[1] = CreateTemplate()
	SetTemplateEmitterBlend(ParticleEffect[1], 1)
	SetTemplateInterval(ParticleEffect[1], 1)
	SetTemplateEmitterLifeTime(ParticleEffect[1], 3)
	SetTemplateParticleLifeTime(ParticleEffect[1], 30, 45)
	SetTemplateTexture(ParticleEffect[1], "GFX\smoke2.png", 2, 1)
	SetTemplateOffset(ParticleEffect[1], 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
	SetTemplateVelocity(ParticleEffect[1], 0.0, 0.0, 0.02, 0.025, 0.0, 0.0)
	SetTemplateAlphaVel(ParticleEffect[1], True)
	SetTemplateSize(ParticleEffect[1], 0.4, 0.4, 0.5, 1.5)
	SetTemplateSizeVel(ParticleEffect[1], .01, 1.01)
	
	;Smoke effect (for decontamination gas)
	ParticleEffect[2] = CreateTemplate()
	SetTemplateEmitterBlend(ParticleEffect[2], 1)
	SetTemplateInterval(ParticleEffect[2], 1)
	SetTemplateEmitterLifeTime(ParticleEffect[2], 3)
	SetTemplateParticleLifeTime(ParticleEffect[2], 30, 45)
	SetTemplateTexture(ParticleEffect[2], "GFX\smoke.png", 2, 1)
	SetTemplateOffset(ParticleEffect[2], -0.1, 0.1, -0.1, 0.1, -0.1, 0.1)
	SetTemplateVelocity(ParticleEffect[2], -0.005, 0.005, 0.0, -0.03, -0.005, 0.005)
	SetTemplateAlphaVel(ParticleEffect[2], True)
	SetTemplateSize(ParticleEffect[2], 0.4, 0.4, 0.5, 1.5)
	SetTemplateSizeVel(ParticleEffect[2], .01, 1.01)
	SetTemplateGravity(ParticleEffect[2], 0.005)
	t0 = CreateTemplate()
	SetTemplateEmitterBlend(t0, 1)
	SetTemplateInterval(t0, 1)
	SetTemplateEmitterLifeTime(t0, 3)
	SetTemplateParticleLifeTime(t0, 30, 45)
	SetTemplateTexture(t0, "GFX\smoke2.png", 2, 1)
	SetTemplateOffset(t0, -0.1, 0.1, -0.1, 0.1, -0.1, 0.1)
	SetTemplateVelocity(t0, -0.005, 0.005, 0.0, -0.03, -0.005, 0.005)
	SetTemplateAlphaVel(t0, True)
	SetTemplateSize(t0, 0.4, 0.4, 0.5, 1.5)
	SetTemplateSizeVel(t0, .01, 1.01)
	SetTemplateGravity(ParticleEffect[2], 0.005)
	SetTemplateSubTemplate(ParticleEffect[2], t0)
	
	DrawLoading(30)
	
	CatchErrors("LoadEntities")
End Function

Function InitNewGame()
	CatchErrors("Uncaught (InitNewGame)")
	
	Local i%, de.Decals, d.Doors, it.Items, r.Rooms, sc.SecurityCams, e.Events
	
	loadingasset = "Loading assets"
	DrawLoading(45)
	
	SelectedEnding = -1
	HideDistance# = 15.0
	
	HeartBeatRate = 70
	
	AccessCode = 0
	For i = 0 To 3
		AccessCode = AccessCode + Rand(1,9)*(10^i)
	Next	
	
	If SelectedMap = "" Then
		CreateMap()
	Else
		LoadMap("Map Creator\Maps\"+SelectedMap)
	EndIf
	InitWayPoints()
	
	DrawLoading(79)
	
	Curr173 = CreateNPC(NPCtype173, 0, -30.0, 0)
	Curr106 = CreateNPC(NPCtypeOldMan, 0, -30.0, 0)
	Curr106\State = 70 * 60 * Rand(12,17)
	
	For d.Doors = Each Doors
		EntityParent(d\obj, 0)
		If d\obj2 <> 0 Then EntityParent(d\obj2, 0)
		If d\frameobj <> 0 Then EntityParent(d\frameobj, 0)
		If d\buttons[0] <> 0 Then EntityParent(d\buttons[0], 0)
		If d\buttons[1] <> 0 Then EntityParent(d\buttons[1], 0)
		
		If d\obj2 <> 0 And d\dir = 0 Then
			MoveEntity(d\obj, 0, 0, 8.0 * RoomScale)
			MoveEntity(d\obj2, 0, 0, 8.0 * RoomScale)
		EndIf	
	Next
	
	For it.Items = Each Items
		EntityType (it\collider, HIT_ITEM)
		EntityParent(it\collider, 0)
	Next
	
	loadingasset = "Loading rooms"
	DrawLoading(80)
	For sc.SecurityCams= Each SecurityCams
		sc\angle = EntityYaw(sc\obj) + sc\angle
		EntityParent(sc\obj, 0)
	Next	
	
	For r.Rooms = Each Rooms
		For i = 0 To MaxRoomLights-1
			If r\Lights[i]<>0 Then EntityParent(r\Lights[i],0)
		Next
		
		If (Not r\RoomTemplate\DisableDecals) Then
			If Rand(4) = 1 Then
				de.Decals = CreateDecal(Rand(2, 3), EntityX(r\obj)+Rnd(- 2,2), 0.003, EntityZ(r\obj)+Rnd(-2,2), 90, Rand(360), 0)
				de\Size = Rnd(0.1, 0.4) : ScaleSprite(de\obj, de\Size, de\Size)
				EntityAlpha(de\obj, Rnd(0.85, 0.95))
			EndIf
			
			If Rand(4) = 1 Then
				de.Decals = CreateDecal(0, EntityX(r\obj)+Rnd(- 2,2), 0.003, EntityZ(r\obj)+Rnd(-2,2), 90, Rand(360), 0)
				de\Size = Rnd(0.5, 0.7) : EntityAlpha(de\obj, 0.7) : de\ID = 1 : ScaleSprite(de\obj, de\Size, de\Size)
				EntityAlpha(de\obj, Rnd(0.7, 0.85))
			EndIf
		EndIf
		
		If (r\RoomTemplate\Name = "start" And IntroEnabled = False) Then 
			PositionEntity (Collider, EntityX(r\obj)+3584*RoomScale, 704*RoomScale, EntityZ(r\obj)+1024*RoomScale)
			PlayerRoom = r
			it = CreateItem("Class D Orientation Leaflet", "paper", 1, 1, 1)
			it\Picked = True
			it\Dropped = -1
			it\itemtemplate\found=True
			Inventory[0] = it
			HideEntity(it\collider)
			EntityType (it\collider, HIT_ITEM)
			EntityParent(it\collider, 0)
			ItemAmount = ItemAmount + 1
			it = CreateItem("Document SCP-173", "paper", 1, 1, 1)
			it\Picked = True
			it\Dropped = -1
			it\itemtemplate\found=True
			Inventory[1] = it
			HideEntity(it\collider)
			EntityType (it\collider, HIT_ITEM)
			EntityParent(it\collider, 0)
			ItemAmount = ItemAmount + 1
		ElseIf (r\RoomTemplate\Name = "173" And IntroEnabled) Then
			PositionEntity (Collider, EntityX(r\obj), 1.0, EntityZ(r\obj))
			PlayerRoom = r
		EndIf
	Next
	
	Local rt.RoomTemplates
	
	For rt.RoomTemplates = Each RoomTemplates
		FreeEntity (rt\obj)
	Next	
	
	Local tw.TempWayPoints
	
	For tw.TempWayPoints = Each TempWayPoints
		Delete tw
	Next
	
	TurnEntity(Collider, 0, Rand(160, 200), 0)
	
	ResetEntity Collider
	
	If SelectedMap = "" Then InitEvents()
	
	For e.Events = Each Events
		If e\EventName = "room2nuke"
			e\EventState = 1
			DebugLog "room2nuke"
		EndIf
		If e\EventName = "room106"
			e\EventState2 = 1
			DebugLog "room106"
		EndIf	
		If e\EventName = "room2sl"
			e\EventState3 = 1
			DebugLog "room2sl"
		EndIf
	Next
	
	MoveMouse viewport_center_x,viewport_center_y
	
	AASetFont Font1
	
	HidePointer()
	
	BlinkTimer = -10
	BlurTimer = 100
	Stamina = 100
	
	For i% = 0 To 70
		FPSfactor = 1.0
		FlushKeys()
		MovePlayer()
		UpdateDoors()
		UpdateNPCs()
		UpdateWorld()
		If (Int(Float(i)*0.27)<>Int(Float(i-1)*0.27)) Then
			DrawLoading(80+Int(Float(i)*0.27))
		EndIf
	Next
	
	FreeTextureCache
	DrawLoading(100)
	
	FlushKeys
	FlushMouse
	
	DropSpeed = 0
	
	PrevTime = MilliSecs()
	
	CatchErrors("InitNewGame")
End Function

Function InitLoadGame()
	CatchErrors("Uncaught (InitLoadGame)")
	
	Local d.Doors, sc.SecurityCams, rt.RoomTemplates, e.Events
	
	SelectedEnding = -1
	
	DrawLoading(80)
	
	For d.Doors = Each Doors
		EntityParent(d\obj, 0)
		If d\obj2 <> 0 Then EntityParent(d\obj2, 0)
		If d\frameobj <> 0 Then EntityParent(d\frameobj, 0)
		If d\buttons[0] <> 0 Then EntityParent(d\buttons[0], 0)
		If d\buttons[1] <> 0 Then EntityParent(d\buttons[1], 0)
	Next
	
	For sc.SecurityCams = Each SecurityCams
		sc\angle = EntityYaw(sc\obj) + sc\angle
		EntityParent(sc\obj, 0)
	Next
	
	ResetEntity Collider
	
	DrawLoading(90)
	
	MoveMouse viewport_center_x,viewport_center_y
	
	AASetFont Font1
	
	HidePointer ()
	
	BlinkTimer = BLINKFREQ
	Stamina = 100
	
	For rt.RoomTemplates = Each RoomTemplates
		If rt\obj <> 0 Then FreeEntity(rt\obj) : rt\obj = 0
	Next
	
	DropSpeed = 0.0
	
	For e.Events = Each Events
		;Loading the necessary stuff for dimension1499, but this will only be done if the player is in this dimension already
		If e\EventName = "dimension1499"
			If e\EventState = 2
				;[Block]
				DrawLoading(91)
				e\room\Objects[0] = CreatePlane()
				Local planetex% = LoadTexture_Strict("GFX\map\dimension1499\grit3.jpg")
				EntityTexture e\room\Objects[0],planetex%
				FreeTexture planetex%
				PositionEntity e\room\Objects[0],0,EntityY(e\room\obj),0
				EntityType e\room\Objects[0],HIT_MAP
				DrawLoading(92)
				NTF_1499Sky = sky_CreateSky("GFX\map\sky\1499sky")
				DrawLoading(93)
				For i = 1 To 15
					e\room\Objects[i] = LoadMesh_Strict("GFX\map\dimension1499\1499object"+i+".b3d")
					HideEntity e\room\Objects[i]
				Next
				DrawLoading(96)
				CreateChunkParts(e\room)
				DrawLoading(97)
				x# = EntityX(e\room\obj)
				z# = EntityZ(e\room\obj)
				Local ch.Chunk
				For i = -2 To 2 Step 2
					ch = CreateChunk(-1,x#*(i*2.5),EntityY(e\room\obj),z#)
				Next
				DrawLoading(98)
				UpdateChunks(e\room,15,False)
				DebugLog "Loaded dimension1499 successful"
				Exit
				;[End Block]
			EndIf
		EndIf
	Next
	
	FreeTextureCache
	
	CatchErrors("InitLoadGame")
	DrawLoading(100)
	
	PrevTime = MilliSecs()
	FPSfactor = 0
	ResetInput()
End Function

Function NullGame(playbuttonsfx%=True)
	CatchErrors("Uncaught (NullGame)")
	Local i%, x%, y%, lvl
	Local itt.ItemTemplates, s.Screens, lt.LightTemplates, d.Doors, m.Materials
	Local wp.WayPoints, twp.TempWayPoints, r.Rooms, it.Items
	
	KillSounds()
	If playbuttonsfx Then PlaySound_Strict ButtonSFX
	
	FreeParticles()
	
	ClearTextureCache
	
	DebugHUD = False
	
	UnableToMove% = False
	
	QuickLoadPercent = -1
	QuickLoadPercent_DisplayTimer# = 0
	QuickLoad_CurrEvent = Null
	
	DeathMSG$=""
	
	SelectedMap = ""
	
	UsedConsole = False
	
	DoorTempID = 0
	RoomTempID = 0
	
	GameSaved = 0
	
	HideDistance# = 15.0
	
	For x = 0 To MapSize+1
		For y = 0 To MapSize+1
			MapTemp(x, y) = 0
			MapFound(x, y) = 0
		Next
	Next
	
	For itt.ItemTemplates = Each ItemTemplates
		itt\found = False
	Next
	
	DropSpeed = 0
	CurrSpeed = 0
	
	DeathTimer=0
	
	HeartBeatVolume = 0
	
	StaminaEffect = 1.0
	StaminaEffectTimer = 0
	BlinkEffect = 1.0
	BlinkEffectTimer = 0
	
	Bloodloss = 0
	Injuries = 0
	Infect = 0
	
	For i = 0 To 5
		SCP1025state[i] = 0
	Next
	
	SelectedEnding = -1
	EndingTimer = 0
	ExplosionTimer = 0
	
	CameraShake = 0
	Shake = 0
	LightFlash = 0
	
	GodMode = 0
	NoClip = 0
	WireframeState = 0
	WireFrame 0
	WearingGasMask = 0
	WearingHazmat = 0
	WearingVest = 0
	Wearing714 = 0
	If WearingNightVision Then
		CameraFogFar = StoredCameraFogFar
		WearingNightVision = 0
	EndIf
	I_427\Using = 0
	I_427\Timer = 0.0
	
	ForceMove = 0.0
	ForceAngle = 0.0	
	Playable = True
	
	CoffinDistance = 100
	
	Contained106 = False
	If Curr173 <> Null Then Curr173\Idle = False
	
	MTFtimer = 0
	
	For s.Screens = Each Screens
		If s\img <> 0 Then FreeImage s\img : s\img = 0
		Delete s
	Next
	
	For i = 0 To MAXACHIEVEMENTS-1
		Achievements[i]=0
	Next
	RefinedItems = 0
	
	ConsoleInput = ""
	ConsoleOpen = False
	
	EyeIrritation = 0
	EyeStuck = 0
	
	ShouldPlay = 0
	
	KillTimer = 0
	FallTimer = 0
	Stamina = 100
	BlurTimer = 0
	SuperMan = False
	SuperManTimer = 0
	Sanity = 0
	RestoreSanity = True
	Crouch = False
	CrouchState = 0.0
	LightVolume = 0.0
	Vomit = False
	VomitTimer = 0.0
	SecondaryLightOn# = True
	PrevSecondaryLightOn# = True
	RemoteDoorOn = True
	SoundTransmission = False
	
	InfiniteStamina% = False
	
	Msg = ""
	MsgTimer = 0
	
	SelectedItem = Null
	
	For i = 0 To MaxItemAmount - 1
		Inventory[i] = Null
	Next
	SelectedItem = Null
	
	ClosestButton = 0
	
	For d.Doors = Each Doors
		Delete d
	Next
	
	For lt.LightTemplates = Each LightTemplates
		Delete lt
	Next 
	
	For m.Materials = Each Materials
		Delete m
	Next
	
	For wp.WayPoints = Each WayPoints
		Delete wp
	Next
	
	For twp.TempWayPoints = Each TempWayPoints
		Delete twp
	Next	
	
	For r.Rooms = Each Rooms
		Delete r
	Next
	
	For itt.ItemTemplates = Each ItemTemplates
		Delete itt
	Next 
	
	For it.Items = Each Items
		Delete it
	Next
	
	For pr.Props = Each Props
		Delete pr
	Next
	
	For de.decals = Each Decals
		Delete de
	Next
	
	For n.NPCS = Each NPCs
		Delete n
	Next
	Curr173 = Null
	Curr106 = Null
	Curr096 = Null
	ForestNPC = 0
	ForestNPCTex = 0
	
	Local e.Events
	
	For e.Events = Each Events
		If e\Sound<>0 Then FreeSound_Strict e\Sound
		If e\Sound2<>0 Then FreeSound_Strict e\Sound2
		Delete e
	Next
	
	For sc.securitycams = Each SecurityCams
		Delete sc
	Next
	
	For em.emitters = Each Emitters
		Delete em
	Next	
	
	For p.particles = Each Particles
		Delete p
	Next
	
	For rt.RoomTemplates = Each RoomTemplates
		rt\obj = 0
	Next
	
	For i = 0 To 6
		If RadioCHN[i] <> 0 Then
			If ChannelPlaying(RadioCHN[i]) Then StopChannel(RadioCHN[i])
		EndIf
	Next
	
	NTF_1499PrevX# = 0.0
	NTF_1499PrevY# = 0.0
	NTF_1499PrevZ# = 0.0
	NTF_1499PrevRoom = Null
	NTF_1499X# = 0.0
	NTF_1499Y# = 0.0
	NTF_1499Z# = 0.0
	Wearing1499% = False
	DeleteChunks()
	
	DeleteDevilEmitters()
	
	NoTarget% = False
	
	OptionsMenu% = -1
	QuitMSG% = -1
	AchievementsMenu% = -1
	
	MusicVolume# = PrevMusicVolume
	SFXVolume# = PrevSFXVolume
	DeafPlayer% = False
	DeafTimer# = 0.0
	
	IsZombie% = False
	
	Delete Each AchievementMsg
	CurrAchvMSGID = 0
	
	ClearWorld
	ReloadAAFont()
	Camera = 0
	ark_blur_cam = 0
	Collider = 0
	Sky = 0
	InitFastResize()
	
	CatchErrors("NullGame")
End Function

Include "Source Code\Save_Core.bb"

; ~ SCP-914 Constants
;[Block]
Const ROUGH% = -2
Const COARSE% = -1
Const ONE_TO_ONE% = 0
Const FINE% = 1
Const VERY_FINE% = 2
;[End Block]

Function Use914(item.Items, setting%, x#, y#, z#)
	RefinedItems = RefinedItems+1
	
	Local it2.Items
	
	Select item\itemtemplate\name
		Case "Gas Mask", "Heavy Gas Mask"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case ONE_TO_ONE
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)
				Case FINE, VERY_FINE
					it2 = CreateItem("Gas Mask", "supergasmask", x, y, z)
					RemoveItem(item)
			End Select
		Case "SCP-1499"
				Select setting
					Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case ONE_TO_ONE
					it2 = CreateItem("Gas Mask", "gasmask", x, y, z)
					RemoveItem(item)
				Case FINE
					it2 = CreateItem("SCP-1499", "super1499", x, y, z)
					RemoveItem(item)
				Case VERY_FINE
					n.NPCs = CreateNPC(NPCtype1499,x,y,z)
					n\State = 1
					n\Sound = LoadSound_Strict("SFX\SCP\1499\Triggered.ogg")
					n\SoundChn = PlaySound2(n\Sound, Camera, n\Collider,20.0)
					n\State3 = 1
					RemoveItem(item)
			End Select
		Case "Ballistic Vest"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case ONE_TO_ONE
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)
				Case FINE
					it2 = CreateItem("Heavy Ballistic Vest", "finevest", x, y, z)
					RemoveItem(item)
				Case VERY_FINE
					it2 = CreateItem("Bulky Ballistic Vest", "veryfinevest", x, y, z)
					RemoveItem(item)
			End Select
		Case "Clipboard"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					For i% = 0 To 19
						If item\SecondInv[i]<>Null Then RemoveItem(item\SecondInv[i])
						item\SecondInv[i]=Null
					Next
					RemoveItem(item)
				Case ONE_TO_ONE
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)
				Case FINE
					item\invSlots = Max(item\state2,15)
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)
				Case VERY_FINE
					item\invSlots = Max(item\state2,20)
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)
			End Select
		Case "Cowbell"
			Select setting
				Case ROUGH,COARSE
					d.Decals = CreateDecal(0, x, 8*RoomScale+0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case ONE_TO_ONE,FINE,VERY_FINE
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)
			End Select
		Case "Night Vision Goggles"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case ONE_TO_ONE
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)
				Case FINE
					it2 = CreateItem("Night Vision Goggles", "finenvgoggles", x, y, z)
					RemoveItem(item)
				Case VERY_FINE
					it2 = CreateItem("Night Vision Goggles", "supernv", x, y, z)
					it2\state = 1000
					RemoveItem(item)
			End Select
		Case "Metal Panel", "SCP-148 Ingot"
			Select setting
				Case ROUGH, COARSE
					it2 = CreateItem("SCP-148 Ingot", "scp148ingot", x, y, z)
					RemoveItem(item)
				Case ONE_TO_ONE, FINE, VERY_FINE
					it2 = Null
					For it.Items = Each Items
						If it<>item And it\collider <> 0 And it\Picked = False Then
							If DistanceSquared(EntityX(it\collider,True), EntityX(item\collider, True),EntityZ(it\collider,True), EntityZ(item\collider, True)) < PowTwo(180.0 * RoomScale) Then
								it2 = it
								Exit
							ElseIf DistanceSquared(EntityX(it\collider,True), x,EntityZ(it\collider,True),z) < PowTwo(180.0 * RoomScale)
								it2 = it
								Exit
							EndIf
						EndIf
					Next
					
					If it2<>Null Then
						Select it2\itemtemplate\tempname
							Case "gasmask", "supergasmask"
								RemoveItem (it2)
								RemoveItem (item)
								
								it2 = CreateItem("Heavy Gas Mask", "gasmask3", x, y, z)
							Case "vest"
								RemoveItem (it2)
								RemoveItem(item)
								it2 = CreateItem("Heavy Ballistic Vest", "finevest", x, y, z)
							Case "hazmatsuit","hazmatsuit2"
								RemoveItem (it2)
								RemoveItem(item)
								it2 = CreateItem("Heavy Hazmat Suit", "hazmatsuit3", x, y, z)
						End Select
					Else 
						If item\itemtemplate\name="SCP-148 Ingot" Then
							it2 = CreateItem("Metal Panel", "scp148", x, y, z)
							RemoveItem(item)
						Else
							PositionEntity(item\collider, x, y, z)
							ResetEntity(item\collider)							
						EndIf
					EndIf					
			End Select
		Case "Severed Hand", "Black Severed Hand"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(3, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE, FINE, VERY_FINE
					If (item\itemtemplate\name = "Severed Hand")
						it2 = CreateItem("Black Severed Hand", "hand2", x, y, z)
					Else
						it2 = CreateItem("Severed Hand", "hand", x, y, z)
					EndIf
			End Select
			RemoveItem(item)
		Case "First Aid Kit", "Blue First Aid Kit"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
				If Rand(2)=1 Then
					it2 = CreateItem("Blue First Aid Kit", "firstaid2", x, y, z)
				Else
					it2 = CreateItem("First Aid Kit", "firstaid", x, y, z)
				EndIf
			Case FINE
					it2 = CreateItem("Small First Aid Kit", "finefirstaid", x, y, z)
				Case VERY_FINE
					it2 = CreateItem("Strange Bottle", "veryfinefirstaid", x, y, z)
			End Select
			RemoveItem(item)
		Case "Level 1 Key Card", "Level 2 Key Card", "Level 3 Key Card", "Level 4 Key Card", "Level 5 Key Card"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					it2 = CreateItem("Playing Card", "misc", x, y, z)
				Case FINE
					Select item\itemtemplate\name
						Case "Level 1 Key Card"
							Select SelectedDifficulty\otherFactors
								Case EASY
									it2 = CreateItem("Level 2 Key Card", "key2", x, y, z)
								Case NORMAL
									If Rand(5)=1 Then
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									Else
										it2 = CreateItem("Level 2 Key Card", "key2", x, y, z)
									EndIf
								Case HARD
									If Rand(4)=1 Then
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									Else
										it2 = CreateItem("Level 2 Key Card", "key2", x, y, z)
									EndIf
							End Select
						Case "Level 2 Key Card"
							Select SelectedDifficulty\otherFactors
								Case EASY
									it2 = CreateItem("Level 3 Key Card", "key3", x, y, z)
								Case NORMAL
									If Rand(4)=1 Then
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									Else
										it2 = CreateItem("Level 3 Key Card", "key3", x, y, z)
									EndIf
								Case HARD
									If Rand(3)=1 Then
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									Else
										it2 = CreateItem("Level 3 Key Card", "key3", x, y, z)
									EndIf
							End Select
						Case "Level 3 Key Card"
							Select SelectedDifficulty\otherFactors
								Case EASY
									If Rand(10)=1 Then
										it2 = CreateItem("Level 4 Key Card", "key4", x, y, z)
									Else
										it2 = CreateItem("Playing Card", "misc", x, y, z)	
									EndIf
								Case NORMAL
									If Rand(15)=1 Then
										it2 = CreateItem("Level 4 Key Card", "key4", x, y, z)
									Else
										it2 = CreateItem("Playing Card", "misc", x, y, z)	
									EndIf
								Case HARD
									If Rand(20)=1 Then
										it2 = CreateItem("Level 4 Key Card", "key4", x, y, z)
									Else
										it2 = CreateItem("Playing Card", "misc", x, y, z)	
									EndIf
							End Select
						Case "Level 4 Key Card"
							Select SelectedDifficulty\otherFactors
								Case EASY
									it2 = CreateItem("Level 5 Key Card", "key5", x, y, z)
								Case NORMAL
									If Rand(4)=1 Then
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									Else
										it2 = CreateItem("Level 5 Key Card", "key5", x, y, z)
									EndIf
								Case HARD
									If Rand(3)=1 Then
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									Else
										it2 = CreateItem("Level 5 Key Card", "key5", x, y, z)
									EndIf
							End Select
						Case "Level 5 Key Card"	
							Local CurrAchvAmount%=0
							
							For i = 0 To MAXACHIEVEMENTS-1
								If Achievements[i]=True
									CurrAchvAmount=CurrAchvAmount+1
								EndIf
							Next
							
							DebugLog CurrAchvAmount
							
							Select SelectedDifficulty\otherFactors
								Case EASY
									If Rand(0,((MAXACHIEVEMENTS-1)*3)-((CurrAchvAmount-1)*3))=0
										it2 = CreateItem("Key Card Omni", "key6", x, y, z)
									Else
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									EndIf
								Case NORMAL
									If Rand(0,((MAXACHIEVEMENTS-1)*4)-((CurrAchvAmount-1)*3))=0
										it2 = CreateItem("Key Card Omni", "key6", x, y, z)
									Else
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									EndIf
								Case HARD
									If Rand(0,((MAXACHIEVEMENTS-1)*5)-((CurrAchvAmount-1)*3))=0
										it2 = CreateItem("Key Card Omni", "key6", x, y, z)
									Else
										it2 = CreateItem("Mastercard", "misc", x, y, z)
									EndIf
							End Select		
					End Select
				Case VERY_FINE
					CurrAchvAmount%=0
					For i = 0 To MAXACHIEVEMENTS-1
						If Achievements[i]=True
							CurrAchvAmount=CurrAchvAmount+1
						EndIf
					Next
					
					DebugLog CurrAchvAmount
					
					Select SelectedDifficulty\otherFactors
						Case EASY
							If Rand(0,((MAXACHIEVEMENTS-1)*3)-((CurrAchvAmount-1)*3))=0
								it2 = CreateItem("Key Card Omni", "key6", x, y, z)
							Else
								it2 = CreateItem("Mastercard", "misc", x, y, z)
							EndIf
						Case NORMAL
							If Rand(0,((MAXACHIEVEMENTS-1)*4)-((CurrAchvAmount-1)*3))=0
								it2 = CreateItem("Key Card Omni", "key6", x, y, z)
							Else
								it2 = CreateItem("Mastercard", "misc", x, y, z)
							EndIf
						Case HARD
							If Rand(0,((MAXACHIEVEMENTS-1)*5)-((CurrAchvAmount-1)*3))=0
								it2 = CreateItem("Key Card Omni", "key6", x, y, z)
							Else
								it2 = CreateItem("Mastercard", "misc", x, y, z)
							EndIf
					End Select
			End Select
			RemoveItem(item)
		Case "Key Card Omni"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					If Rand(2)=1 Then
						it2 = CreateItem("Mastercard", "misc", x, y, z)
					Else
						it2 = CreateItem("Playing Card", "misc", x, y, z)			
					EndIf	
				Case FINE, VERY_FINE
					it2 = CreateItem("Key Card Omni", "key6", x, y, z)
			End Select			
			
			RemoveItem(item)
		Case "Playing Card", "Coin", "Quarter"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					it2 = CreateItem("Level 1 Key Card", "key1", x, y, z)	
				Case FINE, VERY_FINE
					it2 = CreateItem("Level 2 Key Card", "key2", x, y, z)
			End Select
			RemoveItem(item)
		Case "Mastercard"
			Select setting
				Case ROUGH
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
				Case COARSE
					it2 = CreateItem("Quarter", "25ct", x, y, z)
					Local it3.Items,it4.Items,it5.Items
					it3 = CreateItem("Quarter", "25ct", x, y, z)
					it4 = CreateItem("Quarter", "25ct", x, y, z)
					it5 = CreateItem("Quarter", "25ct", x, y, z)
					EntityType (it3\collider, HIT_ITEM)
					EntityType (it4\collider, HIT_ITEM)
					EntityType (it5\collider, HIT_ITEM)
				Case ONE_TO_ONE
					it2 = CreateItem("Level 1 Key Card", "key1", x, y, z)	
				Case FINE, VERY_FINE
					it2 = CreateItem("Level 2 Key Card", "key2", x, y, z)
			End Select
			RemoveItem(item)
		Case "S-NAV 300 Navigator", "S-NAV 310 Navigator", "S-NAV Navigator", "S-NAV Navigator Ultimate"
			Select setting
				Case ROUGH, COARSE
					it2 = CreateItem("Electronical components", "misc", x, y, z)
				Case ONE_TO_ONE
					it2 = CreateItem("S-NAV Navigator", "nav", x, y, z)
					it2\state = 100
				Case FINE
					it2 = CreateItem("S-NAV 310 Navigator", "nav", x, y, z)
					it2\state = 100
				Case VERY_FINE
					it2 = CreateItem("S-NAV Navigator Ultimate", "nav", x, y, z)
					it2\state = 101
			End Select
			RemoveItem(item)
		Case "Radio Transceiver"
			Select setting
				Case ROUGH, COARSE
					it2 = CreateItem("Electronical components", "misc", x, y, z)
				Case ONE_TO_ONE
					it2 = CreateItem("Radio Transceiver", "18vradio", x, y, z)
					it2\state = 100
				Case FINE
					it2 = CreateItem("Radio Transceiver", "fineradio", x, y, z)
					it2\state = 101
				Case VERY_FINE
					it2 = CreateItem("Radio Transceiver", "veryfineradio", x, y, z)
					it2\state = 101
			End Select
			RemoveItem(item)
		Case "SCP-513"
			Select setting
				Case ROUGH, COARSE
					PlaySound_Strict LoadTempSound("SFX\SCP\513\914Refine.ogg")
					For n.npcs = Each NPCs
						If n\npctype = NPCtype5131 Then RemoveNPC(n)
					Next
					d.Decals = CreateDecal(0, x, 8*RoomScale+0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE, FINE, VERY_FINE
					it2 = CreateItem("SCP-513", "scp513", x, y, z)
			End Select
			RemoveItem(item)
		Case "Some SCP-420-J", "Cigarette"
			Select setting
				Case ROUGH, COARSE			
					d.Decals = CreateDecal(0, x, 8*RoomScale+0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					it2 = CreateItem("Cigarette", "cigarette", x + 1.5, y + 0.5, z + 1.0)
				Case FINE
					it2 = CreateItem("Joint", "420s", x + 1.5, y + 0.5, z + 1.0)
				Case VERY_FINE
					it2 = CreateItem("Smelly Joint", "420s", x + 1.5, y + 0.5, z + 1.0)
			End Select
			RemoveItem(item)
		Case "9V Battery", "18V Battery", "Strange Battery"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					it2 = CreateItem("18V Battery", "18vbat", x, y, z)
				Case FINE
					it2 = CreateItem("Strange Battery", "killbat", x, y, z)
				Case VERY_FINE
					it2 = CreateItem("Strange Battery", "killbat", x, y, z)
			End Select
			RemoveItem(item)
		Case "ReVision Eyedrops", "RedVision Eyedrops", "Eyedrops"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					it2 = CreateItem("RedVision Eyedrops", "eyedrops", x,y,z)
				Case FINE
					it2 = CreateItem("Eyedrops", "fineeyedrops", x,y,z)
				Case VERY_FINE
					it2 = CreateItem("Eyedrops", "supereyedrops", x,y,z)
			End Select
			RemoveItem(item)		
		Case "Hazmat Suit"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					it2 = CreateItem("Hazmat Suit", "hazmatsuit", x,y,z)
				Case FINE
					it2 = CreateItem("Hazmat Suit", "hazmatsuit2", x,y,z)
				Case VERY_FINE
					it2 = CreateItem("Hazmat Suit", "hazmatsuit2", x,y,z)
			End Select
			RemoveItem(item)
		Case "Syringe"
			Select item\itemtemplate\tempname
				Case "syringe"
					Select setting
						Case ROUGH, COARSE
							d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
							d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
						Case ONE_TO_ONE
							it2 = CreateItem("Small First Aid Kit", "finefirstaid", x, y, z)	
						Case FINE
							it2 = CreateItem("Syringe", "finesyringe", x, y, z)
						Case VERY_FINE
							it2 = CreateItem("Syringe", "veryfinesyringe", x, y, z)
					End Select
				Case "finesyringe"
					Select setting
						Case ROUGH
							d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
							d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
						Case COARSE
							it2 = CreateItem("First Aid Kit", "firstaid", x, y, z)
						Case ONE_TO_ONE
							it2 = CreateItem("Blue First Aid Kit", "firstaid2", x, y, z)	
						Case FINE, VERY_FINE
							it2 = CreateItem("Syringe", "veryfinesyringe", x, y, z)
					End Select
				Case "veryfinesyringe"
					Select setting
						Case ROUGH, COARSE, ONE_TO_ONE, FINE
							it2 = CreateItem("Electronical components", "misc", x, y, z)	
						Case VERY_FINE
							n.NPCs = CreateNPC(NPCtype008,x,y,z)
							n\State = 2
					End Select
			End Select
			RemoveItem(item)
		Case "SCP-500-01", "Upgraded pill", "Pill"
			Select setting
				Case ROUGH, COARSE
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case ONE_TO_ONE
					it2 = CreateItem("Pill", "pill", x, y, z)
					RemoveItem(item)
				Case FINE
					Local no427Spawn% = False
					For it3.Items = Each Items
						If it3\itemtemplate\tempname = "scp427" Then
							no427Spawn = True
							Exit
						EndIf
					Next
					If (Not no427Spawn) Then
						it2 = CreateItem("SCP-427", "scp427", x, y, z)
					Else
						it2 = CreateItem("Upgraded pill", "scp500death", x, y, z)
					EndIf
					RemoveItem(item)
				Case VERY_FINE
					it2 = CreateItem("Upgraded pill", "scp500death", x, y, z)
					RemoveItem(item)
			End Select
		Default
			Select item\itemtemplate\tempname
				Case "cup"
					Select setting
						Case ROUGH, COARSE
							d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
							d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
						Case ONE_TO_ONE
							it2 = CreateItem("cup", "cup", x,y,z)
							it2\name = item\name
							it2\r = 255-item\r
							it2\g = 255-item\g
							it2\b = 255-item\b
						Case FINE
							it2 = CreateItem("cup", "cup", x,y,z)
							it2\name = item\name
							it2\state = 1.0
							it2\r = Min(item\r*Rnd(0.9,1.1),255)
							it2\g = Min(item\g*Rnd(0.9,1.1),255)
							it2\b = Min(item\b*Rnd(0.9,1.1),255)
						Case VERY_FINE
							it2 = CreateItem("cup", "cup", x,y,z)
							it2\name = item\name
							it2\state = Max(it2\state*2.0,2.0)	
							it2\r = Min(item\r*Rnd(0.5,1.5),255)
							it2\g = Min(item\g*Rnd(0.5,1.5),255)
							it2\b = Min(item\b*Rnd(0.5,1.5),255)
							If Rand(5)=1 Then
								ExplosionTimer = 135
							EndIf
					End Select	
					RemoveItem(item)
				Case "paper"
					Select setting
						Case ROUGH, COARSE
							d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
							d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
						Case ONE_TO_ONE
							Select Rand(6)
								Case 1
									it2 = CreateItem("Document SCP-106", "paper", x, y, z)
								Case 2
									it2 = CreateItem("Document SCP-079", "paper", x, y, z)
								Case 3
									it2 = CreateItem("Document SCP-173", "paper", x, y, z)
								Case 4
									it2 = CreateItem("Document SCP-895", "paper", x, y, z)
								Case 5
									it2 = CreateItem("Document SCP-682", "paper", x, y, z)
								Case 6
									it2 = CreateItem("Document SCP-860", "paper", x, y, z)
							End Select
						Case FINE, VERY_FINE
							it2 = CreateItem("Origami", "misc", x, y, z)
					End Select
					RemoveItem(item)
				Default
					PositionEntity(item\collider, x, y, z)
					ResetEntity(item\collider)	
			End Select
	End Select
	
	If it2 <> Null Then EntityType (it2\collider, HIT_ITEM)
End Function

Function Use294()
	Local x#,y#, xtemp%,ytemp%, strtemp$, temp%
	
	ShowPointer()
	
	x = GraphicWidth/2 - (ImageWidth(Panel294)/2)
	y = GraphicHeight/2 - (ImageHeight(Panel294)/2)
	DrawImage Panel294, x, y
	If Fullscreen Then DrawImage CursorIMG, ScaledMouseX(),ScaledMouseY()
	
	temp = True
	If PlayerRoom\SoundCHN<>0 Then temp = False
	
	AAText x+907, y+185, Input294, True,True
	
	If temp Then
		If MouseHit1 Then
			xtemp = Floor((ScaledMouseX()-x-228) / 35.5)
			ytemp = Floor((ScaledMouseY()-y-342) / 36.5)
			
			If ytemp => 0 And ytemp < 5 Then
				If xtemp => 0 And xtemp < 10 Then PlaySound_Strict ButtonSFX
			EndIf
			
			strtemp = ""
			
			temp = False
			
			Select ytemp
				Case 0
					strtemp = (xtemp + 1) Mod 10
				Case 1
					Select xtemp
						Case 0
							strtemp = "Q"
						Case 1
							strtemp = "W"
						Case 2
							strtemp = "E"
						Case 3
							strtemp = "R"
						Case 4
							strtemp = "T"
						Case 5
							strtemp = "Y"
						Case 6
							strtemp = "U"
						Case 7
							strtemp = "I"
						Case 8
							strtemp = "O"
						Case 9
							strtemp = "P"
					End Select
				Case 2
					Select xtemp
						Case 0
							strtemp = "A"
						Case 1
							strtemp = "S"
						Case 2
							strtemp = "D"
						Case 3
							strtemp = "F"
						Case 4
							strtemp = "G"
						Case 5
							strtemp = "H"
						Case 6
							strtemp = "J"
						Case 7
							strtemp = "K"
						Case 8
							strtemp = "L"
						Case 9 ;dispense
							temp = True
					End Select
				Case 3
					Select xtemp
						Case 0
							strtemp = "Z"
						Case 1
							strtemp = "X"
						Case 2
							strtemp = "C"
						Case 3
							strtemp = "V"
						Case 4
							strtemp = "B"
						Case 5
							strtemp = "N"
						Case 6
							strtemp = "M"
						Case 7
							strtemp = "-"
						Case 8
							strtemp = " "
						Case 9
							Input294 = Left(Input294, Max(Len(Input294)-1,0))
					End Select
				Case 4
					strtemp = " "
			End Select
			
			Input294 = Input294 + strtemp
			
			Input294 = Left(Input294, Min(Len(Input294),15))
			
			If temp And Input294<>"" Then ;dispense
				Input294 = Trim(Lower(Input294))
				If Left(Input294, Min(7,Len(Input294))) = "cup of " Then
					Input294 = Right(Input294, Len(Input294)-7)
				ElseIf Left(Input294, Min(9,Len(Input294))) = "a cup of " 
					Input294 = Right(Input294, Len(Input294)-9)
				EndIf
				
				If Input294<>""
					Local loc% = GetINISectionLocation("DATA\SCP-294.ini",Input294)
				EndIf
				
				If loc > 0 Then
					strtemp$ = GetINIString2("DATA\SCP-294.ini", loc, "dispensesound")
					If strtemp="" Then
						PlayerRoom\SoundCHN = PlaySound_Strict (LoadTempSound("SFX\SCP\294\dispense1.ogg"))
					Else
						PlayerRoom\SoundCHN = PlaySound_Strict (LoadTempSound(strtemp))
					EndIf
					
					If GetINIInt2("DATA\SCP-294.ini", loc, "explosion")=True Then 
						ExplosionTimer = 135
						DeathMSG = GetINIString2("DATA\SCP-294.ini", loc, "deathmessage")
					EndIf
					
					strtemp$ = GetINIString2("DATA\SCP-294.ini", loc, "color")
					
					sep1 = Instr(strtemp, ",", 1)
					sep2 = Instr(strtemp, ",", sep1+1)
					r% = Trim(Left(strtemp, sep1-1))
					g% = Trim(Mid(strtemp, sep1+1, sep2-sep1-1))
					b% = Trim(Right(strtemp, Len(strtemp)-sep2))
					
					alpha# = Float(GetINIString2("DATA\SCP-294.ini", loc, "alpha",1.0))
					glow = GetINIInt2("DATA\SCP-294.ini", loc, "glow")
					If glow Then alpha = -alpha
					
					it.items = CreateItem("Cup", "cup", EntityX(PlayerRoom\Objects[1],True),EntityY(PlayerRoom\Objects[1],True),EntityZ(PlayerRoom\Objects[1],True), r,g,b,alpha)
					it\name = "Cup of "+Input294
					EntityType (it\collider, HIT_ITEM)
				Else
					;out of range
					Input294 = "OUT OF RANGE"
					PlayerRoom\SoundCHN = PlaySound_Strict (LoadTempSound("SFX\SCP\294\outofrange.ogg"))
				EndIf
			EndIf
		EndIf
		
		If MouseHit2 Lor (Not Using294) Then 
			HidePointer()
			Using294 = False
			Input294 = ""
			MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
		EndIf
	Else ;playing a dispensing sound
		If Input294 <> "OUT OF RANGE" Then Input294 = "DISPENSING..."
		
		If Not ChannelPlaying(PlayerRoom\SoundCHN) Then
			If Input294 <> "OUT OF RANGE" Then
				HidePointer()
				Using294 = False
				MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
				Local e.Events
				For e.Events = Each Events
					If e\room = PlayerRoom
						e\EventState2 = 0
						Exit
					EndIf
				Next
			EndIf
			Input294=""
			PlayerRoom\SoundCHN=0
		EndIf
	EndIf
End Function

Function Use427()
	Local i%,pvt%,de.Decals,tempchn%
	Local prevI427Timer# = I_427\Timer
	
	If I_427\Timer < 70*360
		If I_427\Using=True Then
			I_427\Timer = I_427\Timer + FPSfactor
			If Injuries > 0.0 Then
				Injuries = Max(Injuries - 0.0005 * FPSfactor,0.0)
			EndIf
			If Bloodloss > 0.0 And Injuries <= 1.0 Then
				Bloodloss = Max(Bloodloss - 0.001 * FPSfactor,0.0)
			EndIf
			If Infect > 0.0 Then
				Infect = Max(Infect - 0.001 * FPSfactor,0.0)
			EndIf
			For i = 0 To 5
				If SCP1025state[i]>0.0 Then
					SCP1025state[i] = Max(SCP1025state[i] - 0.001 * FPSfactor,0.0)
				EndIf
			Next
			If I_427\Sound[0]=0 Then
				I_427\Sound[0] = LoadSound_Strict("SFX\SCP\427\Effect.ogg")
			EndIf
			If (Not ChannelPlaying(I_427\SoundCHN[0])) Then
				I_427\SoundCHN[0] = PlaySound_Strict(I_427\Sound[0])
			EndIf
			If I_427\Timer => 70*180 Then
				If I_427\Sound[1]=0 Then
					I_427\Sound[1] = LoadSound_Strict("SFX\SCP\427\Transform.ogg")
				EndIf
				If (Not ChannelPlaying(I_427\SoundCHN[1])) Then
					I_427\SoundCHN[1] = PlaySound_Strict(I_427\Sound[1])
				EndIf
			EndIf
			If prevI427Timer < 70*60 And I_427\Timer => 70*60 Then
				Msg = "You feel refreshed and energetic."
				MsgTimer = 70*5
			ElseIf prevI427Timer < 70*180 And I_427\Timer => 70*180 Then
				Msg = "You feel gentle muscle spasms all over your body."
				MsgTimer = 70*5
			EndIf
		Else
			For i = 0 To 1
				If I_427\SoundCHN[i]<>0 Then
					If ChannelPlaying(I_427\SoundCHN[i]) Then
						StopChannel(I_427\SoundCHN[i])
					EndIf
				EndIf
			Next
		EndIf
	Else
		If prevI427Timer-FPSfactor < 70*360 And I_427\Timer => 70*360 Then
			Msg = "Your muscles are swelling. You feel more powerful than ever."
			MsgTimer = 70*5
		ElseIf prevI427Timer-FPSfactor < 70*390 And I_427\Timer => 70*390 Then
			Msg = "You can't feel your legs. But you don't need legs anymore."
			MsgTimer = 70*5
		EndIf
		I_427\Timer = I_427\Timer + FPSfactor
		If I_427\Sound[0]=0 Then
			I_427\Sound[0] = LoadSound_Strict("SFX\SCP\427\Effect.ogg")
		EndIf
		If I_427\Sound[1]=0 Then
			I_427\Sound[1] = LoadSound_Strict("SFX\SCP\427\Transform.ogg")
		EndIf
		For i = 0 To 1
			If (Not ChannelPlaying(I_427\SoundCHN[i])) Then
				I_427\SoundCHN[i] = PlaySound_Strict(I_427\Sound[i])
			EndIf
		Next
		If Rnd(200)<2.0 Then
			pvt = CreatePivot()
			PositionEntity pvt, EntityX(Collider)+Rnd(-0.05,0.05),EntityY(Collider)-0.05,EntityZ(Collider)+Rnd(-0.05,0.05)
			TurnEntity pvt, 90, 0, 0
			EntityPick(pvt,0.3)
			de.Decals = CreateDecal(20, PickedX(), PickedY()+0.005, PickedZ(), 90, Rand(360), 0)
			de\Size = Rnd(0.03,0.08)*2.0 : EntityAlpha(de\obj, 1.0) : ScaleSprite de\obj, de\Size, de\Size
			tempchn% = PlaySound_Strict (DripSFX[Rand(0,2)])
			ChannelVolume tempchn, Rnd(0.0,0.8)*SFXVolume
			ChannelPitch tempchn, Rand(20000,30000)
			FreeEntity pvt
			BlurTimer = 800
		EndIf
		If I_427\Timer >= 70*420 Then
			Kill()
			DeathMSG = Chr(34)+"Requesting support from MTF Nu-7. We need more firepower to take this thing down."+Chr(34)
		ElseIf I_427\Timer >= 70*390 Then
			Crouch = True
		EndIf
	EndIf
End Function

Function UpdateMTF%()
	If PlayerRoom\RoomTemplate\Name = "gateaentrance" Then Return
	
	Local r.Rooms, n.NPCs
	Local dist#, i%
	
	If MTFtimer = 0 Then
		If Rand(30)=1 And PlayerRoom\RoomTemplate\Name$ <> "dimension1499" Then
			
			Local entrance.Rooms = Null
			For r.Rooms = Each Rooms
				If Lower(r\RoomTemplate\Name) = "gateaentrance" Then entrance = r : Exit
			Next
			
			If entrance <> Null Then 
				If Abs(EntityZ(entrance\obj)-EntityZ(Collider))<30.0 Then
					If PlayerInReachableRoom()
						PlayAnnouncement("SFX\Character\MTF\Announc.ogg")
					EndIf
					
					MTFtimer = FPSfactor
					
					Local leader.NPCs
					
					For i = 0 To 2
						n.NPCs = CreateNPC(NPCtypeMTF, EntityX(entrance\obj)+0.3*(i-1), 1.0,EntityZ(entrance\obj)+8.0)
						
						If i = 0 Then 
							leader = n
						Else
							n\MTFLeader = leader
						EndIf
						
						n\PrevX = i
					Next
				EndIf
			EndIf
		EndIf
	Else
		If MTFtimer <= 70*120
			MTFtimer = MTFtimer + FPSfactor
		ElseIf MTFtimer > 70*120 And MTFtimer < 10000
			If PlayerInReachableRoom()
				PlayAnnouncement("SFX\Character\MTF\AnnouncAfter1.ogg")
			EndIf
			MTFtimer = 10000
		ElseIf MTFtimer >= 10000 And MTFtimer <= 10000+(70*120)
			MTFtimer = MTFtimer + FPSfactor
		ElseIf MTFtimer > 10000+(70*120) And MTFtimer < 20000
			If PlayerInReachableRoom()
				PlayAnnouncement("SFX\Character\MTF\AnnouncAfter2.ogg")
			EndIf
			MTFtimer = 20000
		ElseIf MTFtimer >= 20000 And MTFtimer <= 20000+(70*60)
			MTFtimer = MTFtimer + FPSfactor
		ElseIf MTFtimer > 20000+(70*60) And MTFtimer < 25000
			If PlayerInReachableRoom()
				;If the player has an SCP in their inventory play special voice line.
				For i = 0 To MaxItemAmount-1
					If Inventory[i] <> Null Then
						If (Left(Inventory[i]\itemtemplate\name, 4) = "SCP-") And (Left(Inventory[i]\itemtemplate\name, 7) <> "SCP-035") And (Left(Inventory[i]\itemtemplate\name, 7) <> "SCP-093")
							PlayAnnouncement("SFX\Character\MTF\ThreatAnnouncPossession.ogg")
							MTFtimer = 25000
							Return
							Exit
						EndIf
					EndIf
				Next
				PlayAnnouncement("SFX\Character\MTF\ThreatAnnounc"+Rand(1,3)+".ogg")
			EndIf
			MTFtimer = 25000
		ElseIf MTFtimer >= 25000 And MTFtimer <= 25000+(70*60)
			MTFtimer = MTFtimer + FPSfactor
		ElseIf MTFtimer > 25000+(70*60) And MTFtimer < 30000
			If PlayerInReachableRoom()
				PlayAnnouncement("SFX\Character\MTF\ThreatAnnouncFinal.ogg")
			EndIf
			MTFtimer = 30000
		EndIf
	EndIf
End Function

Function Update008()
	Local temp#, i%, r.Rooms
	Local teleportForInfect% = True
	
	If PlayerRoom\RoomTemplate\Name = "room860"
		For e.Events = Each Events
			If e\EventName = "room860"
				If e\EventState = 1.0
					teleportForInfect = False
				EndIf
				Exit
			EndIf
		Next
	ElseIf PlayerRoom\RoomTemplate\Name = "dimension1499" Lor PlayerRoom\RoomTemplate\Name = "pocketdimension" Lor PlayerRoom\RoomTemplate\Name = "gatea"
		teleportForInfect = False
	ElseIf PlayerRoom\RoomTemplate\Name = "exit1" And EntityY(Collider)>1040.0*RoomScale
		teleportForInfect = False
	EndIf
	
	If Infect>0 Then
		ShowEntity InfectOverlay
		If Infect < 93.0 Then
			temp=Infect
			If (Not I_427\Using And I_427\Timer < 70*360) Then
				Infect = Min(Infect+FPSfactor*0.002,100)
			EndIf
			
			BlurTimer = Max(Infect*3*(2.0-CrouchState),BlurTimer)
			
			HeartBeatRate = Max(HeartBeatRate, 100)
			HeartBeatVolume = Max(HeartBeatVolume, Infect/120.0)
			
			EntityAlpha InfectOverlay, Min(((Infect*0.2)^2)/1000.0,0.5) * (Sin(MilliSecs2()/8.0)+2.0)
			
			For i = 0 To 6
				If Infect>i*15+10 And temp =< i*15+10 Then
					PlaySound_Strict LoadTempSound("SFX\SCP\008\Voices"+i+".ogg")
				EndIf
			Next
			
			If Infect > 20 And temp =< 20.0 Then
				Msg = "You feel kinda feverish."
				MsgTimer = 70*6
			ElseIf Infect > 40 And temp =< 40.0
				Msg = "You feel nauseated."
				MsgTimer = 70*6
			ElseIf Infect > 60 And temp =< 60.0
				Msg = "The nausea's getting worse."
				MsgTimer = 70*6
			ElseIf Infect > 80 And temp =< 80.0
				Msg = "You feel very faint."
				MsgTimer = 70*6
			ElseIf Infect =>91.5
				BlinkTimer = Max(Min(-10*(Infect-91.5),BlinkTimer),-10)
				IsZombie = True
				UnableToMove = True
				If Infect >= 92.7 And temp < 92.7 Then
					If teleportForInfect
						For r.Rooms = Each Rooms
							If r\RoomTemplate\Name="008" Then
								PositionEntity Collider, EntityX(r\Objects[7],True),EntityY(r\Objects[7],True),EntityZ(r\Objects[7],True),True
								ResetEntity Collider
								r\NPC[0] = CreateNPC(NPCtypeD, EntityX(r\Objects[6],True),EntityY(r\Objects[6],True)+0.2,EntityZ(r\Objects[6],True))
								r\NPC[0]\Sound = LoadSound_Strict("SFX\SCP\008\KillScientist1.ogg")
								r\NPC[0]\SoundChn = PlaySound_Strict(r\NPC[0]\Sound)
								tex = LoadTexture_Strict("GFX\npcs\scientist2.jpg")
								EntityTexture r\NPC[0]\obj, tex
								FreeTexture tex
								r\NPC[0]\State=6
								PlayerRoom = r
								UnableToMove = False
								Exit
							EndIf
						Next
					EndIf
				EndIf
			EndIf
		Else
			temp=Infect
			Infect = Min(Infect+FPSfactor*0.004,100)
			
			If teleportForInfect
				If Infect < 94.7 Then
					EntityAlpha InfectOverlay, 0.5 * (Sin(MilliSecs2()/8.0)+2.0)
					BlurTimer = 900
					
					If Infect > 94.5 Then BlinkTimer = Max(Min(-50*(Infect-94.5),BlinkTimer),-10)
					PointEntity Collider, PlayerRoom\NPC[0]\Collider
					PointEntity PlayerRoom\NPC[0]\Collider, Collider
					PointEntity Camera, PlayerRoom\NPC[0]\Collider,EntityRoll(Camera)
					ForceMove = 0.75
					Injuries = 2.5
					Bloodloss = 0
					UnableToMove = False
					
					Animate2(PlayerRoom\NPC[0]\obj, AnimTime(PlayerRoom\NPC[0]\obj), 357, 381, 0.3)
				ElseIf Infect < 98.5
					EntityAlpha InfectOverlay, 0.5 * (Sin(MilliSecs2()/5.0)+2.0)
					BlurTimer = 950
					
					ForceMove = 0.0
					UnableToMove = True
					PointEntity Camera, PlayerRoom\NPC[0]\Collider
					
					If temp < 94.7 Then 
						PlayerRoom\NPC[0]\Sound = LoadSound_Strict("SFX\SCP\008\KillScientist2.ogg")
						PlayerRoom\NPC[0]\SoundChn = PlaySound_Strict(PlayerRoom\NPC[0]\Sound)
						
						DeathMSG = "Subject D-9341 found ingesting Dr. [REDACTED] at Sector [REDACTED]. Subject was immediately terminated by Nine-Tailed Fox and sent for autopsy. "
						DeathMSG = DeathMSG + "SCP-008 infection was confirmed, after which the body was incinerated."
						
						Kill()
						de.Decals = CreateDecal(3, EntityX(PlayerRoom\NPC[0]\Collider), 544*RoomScale + 0.01, EntityZ(PlayerRoom\NPC[0]\Collider),90,Rnd(360),0)
						de\Size = 0.8
						ScaleSprite(de\obj, de\Size,de\Size)
					ElseIf Infect > 96
						BlinkTimer = Max(Min(-10*(Infect-96),BlinkTimer),-10)
					Else
						KillTimer = Max(-350, KillTimer)
					EndIf
					
					If PlayerRoom\NPC[0]\State2=0 Then
						Animate2(PlayerRoom\NPC[0]\obj, AnimTime(PlayerRoom\NPC[0]\obj), 13, 19, 0.3,False)
						If AnimTime(PlayerRoom\NPC[0]\obj) => 19 Then PlayerRoom\NPC[0]\State2=1
					Else
						Animate2(PlayerRoom\NPC[0]\obj, AnimTime(PlayerRoom\NPC[0]\obj), 19, 13, -0.3)
						If AnimTime(PlayerRoom\NPC[0]\obj) =< 13 Then PlayerRoom\NPC[0]\State2=0
					EndIf
					
					If ParticleAmount>0
						If Rand(50)=1 Then
							p.Particles = CreateParticle(EntityX(PlayerRoom\NPC[0]\Collider),EntityY(PlayerRoom\NPC[0]\Collider),EntityZ(PlayerRoom\NPC[0]\Collider), 5, Rnd(0.05,0.1), 0.15, 200)
							p\speed = 0.01
							p\SizeChange = 0.01
							p\A = 0.5
							p\Achange = -0.01
							RotateEntity p\pvt, Rnd(360),Rnd(360),0
						EndIf
					EndIf
					
					PositionEntity Head, EntityX(PlayerRoom\NPC[0]\Collider,True), EntityY(PlayerRoom\NPC[0]\Collider,True)+0.65,EntityZ(PlayerRoom\NPC[0]\Collider,True),True
					RotateEntity Head, (1.0+Sin(MilliSecs2()/5.0))*15, PlayerRoom\angle-180, 0, True
					MoveEntity Head, 0,0,-0.4
					TurnEntity Head, 80+(Sin(MilliSecs2()/5.0))*30,(Sin(MilliSecs2()/5.0))*40,0
				EndIf
			Else
				Kill()
				BlinkTimer = Max(Min(-10*(Infect-96),BlinkTimer),-10)
				If PlayerRoom\RoomTemplate\Name = "dimension1499" Then
					DeathMSG = "The whereabouts of SCP-1499 are still unknown, but a recon team has been dispatched to investigate reports of a violent attack to a church in the Russian town of [REDACTED]."
				ElseIf PlayerRoom\RoomTemplate\Name = "gatea" Lor PlayerRoom\RoomTemplate\Name = "exit1" Then
					DeathMSG = "Subject D-9341 found wandering around Gate "
					If PlayerRoom\RoomTemplate\Name = "gatea" Then
						DeathMSG = DeathMSG + "A"
					Else
						DeathMSG = DeathMSG + "B"
					EndIf
					DeathMSG = DeathMSG + ". Subject was immediately terminated by Nine-Tailed Fox and sent for autopsy. "
					DeathMSG = DeathMSG + "SCP-008 infection was confirmed, after which the body was incinerated."
				Else
					DeathMSG = ""
				EndIf
			EndIf
		EndIf
	Else
		HideEntity InfectOverlay
	EndIf
End Function

;Update any ailments inflicted by SCP-294 drinks.
Function Update294()
	CatchErrors("Uncaught (Update294)")
	
	If CameraShakeTimer > 0 Then
		CameraShakeTimer = CameraShakeTimer - (FPSfactor/70)
		CameraShake = 2
	EndIf
	
	If VomitTimer > 0 Then
		DebugLog VomitTimer
		VomitTimer = VomitTimer - (FPSfactor/70)
		
		If (MilliSecs2() Mod 1600) < Rand(200, 400) Then
			If BlurTimer = 0 Then BlurTimer = Rnd(10, 20)*70
			CameraShake = Rnd(0, 2)
		EndIf
		
		If Rand(50) = 50 And (MilliSecs2() Mod 4000) < 200 Then PlaySound_Strict(CoughSFX[Rand(0,2)])
		
		;Regurgitate when timer is below 10 seconds. (ew)
		If VomitTimer < 10 And Rnd(0, 500 * VomitTimer) < 2 Then
			If (Not ChannelPlaying(VomitCHN)) And (Not Regurgitate) Then
				VomitCHN = PlaySound_Strict(LoadTempSound("SFX\SCP\294\Retch" + Rand(1, 2) + ".ogg"))
				Regurgitate = MilliSecs2() + 50
			EndIf
		EndIf
		
		If Regurgitate > MilliSecs2() And Regurgitate <> 0 Then
			mouse_y_speed_1 = mouse_y_speed_1 + 1.0
		Else
			Regurgitate = 0
		EndIf
		
	ElseIf VomitTimer < 0 Then ;vomit
		VomitTimer = VomitTimer - (FPSfactor/70)
		
		If VomitTimer > -5 Then
			If (MilliSecs2() Mod 400) < 50 Then CameraShake = 4 
			mouse_x_speed_1 = 0.0
			Playable = False
		Else
			Playable = True
		EndIf
		
		If (Not Vomit) Then
			BlurTimer = 40 * 70
			VomitSFX = LoadSound_Strict("SFX\SCP\294\Vomit.ogg")
			VomitCHN = PlaySound_Strict(VomitSFX)
			PrevInjuries = Injuries
			PrevBloodloss = Bloodloss
			Injuries = 1.5
			Bloodloss = 70
			EyeIrritation = 9 * 70
			
			pvt = CreatePivot()
			PositionEntity(pvt, EntityX(Camera), EntityY(Collider) - 0.05, EntityZ(Camera))
			TurnEntity(pvt, 90, 0, 0)
			EntityPick(pvt, 0.3)
			de.decals = CreateDecal(5, PickedX(), PickedY() + 0.005, PickedZ(), 90, 180, 0)
			de\Size = 0.001 : de\SizeChange = 0.001 : de\MaxSize = 0.6 : EntityAlpha(de\obj, 1.0) : EntityColor(de\obj, 0.0, Rnd(200, 255), 0.0) : ScaleSprite de\obj, de\size, de\size
			FreeEntity pvt
			Vomit = True
		EndIf
		
		mouse_y_speed_1 = mouse_y_speed_1 + Max((1.0 + VomitTimer / 10), 0.0)
		
		If VomitTimer < -15 Then
			FreeSound_Strict(VomitSFX)
			VomitTimer = 0
			If KillTimer >= 0 Then
				PlaySound_Strict(BreathSFX(0,0))
			EndIf
			Injuries = PrevInjuries
			Bloodloss = PrevBloodloss
			Vomit = False
		EndIf
	EndIf
	
	CatchErrors("Update294")
End Function

Function UpdateExplosion()
	If ExplosionTimer > 0 Then
		ExplosionTimer = ExplosionTimer+FPSfactor
		
		If ExplosionTimer < 140.0 Then
			If ExplosionTimer-FPSfactor < 5.0 Then
				ExplosionSFX = LoadSound_Strict("SFX\Ending\GateB\Nuke1.ogg")
				PlaySound_Strict ExplosionSFX
				CameraShake = 10.0
				ExplosionTimer = 5.0
			EndIf
			
			CameraShake = CurveValue(ExplosionTimer/60.0,CameraShake, 50.0)
		Else
			CameraShake = Min((ExplosionTimer/20.0),20.0)
			If ExplosionTimer-FPSfactor < 140.0 Then
				BlinkTimer = 1.0
				ExplosionSFX = LoadSound_Strict("SFX\Ending\GateB\Nuke2.ogg")
				PlaySound_Strict ExplosionSFX				
				For i = 0 To (10+(10*(ParticleAmount+1)))
					p.Particles = CreateParticle(EntityX(Collider)+Rnd(-0.5,0.5),EntityY(Collider)-Rnd(0.2,1.5),EntityZ(Collider)+Rnd(-0.5,0.5),0, Rnd(0.2,0.6), 0.0, 350)	
					RotateEntity p\pvt,-90,0,0,True
					p\speed = Rnd(0.05,0.07)
				Next
			EndIf
			LightFlash = Min((ExplosionTimer-140.0)/10.0,5.0)
			
			If ExplosionTimer > 160 Then KillTimer = Min(KillTimer,-0.1)
			If ExplosionTimer > 500 Then ExplosionTimer = 0
			
			;a dirty workaround to prevent the collider from falling down into the facility once the nuke goes off,
			;causing the UpdateEvent function to be called again and crashing the game
			PositionEntity Collider, EntityX(Collider), 200, EntityZ(Collider)
		EndIf
	EndIf
End Function

Function UpdateLeave1499()
	Local r.Rooms, it.Items,r2.Rooms,i%
	Local r1499.Rooms
	
	If (Not Wearing1499) And PlayerRoom\RoomTemplate\Name$ = "dimension1499"
		For r.Rooms = Each Rooms
			If r = NTF_1499PrevRoom
				BlinkTimer = -1
				NTF_1499X# = EntityX(Collider)
				NTF_1499Y# = EntityY(Collider)
				NTF_1499Z# = EntityZ(Collider)
				PositionEntity (Collider, NTF_1499PrevX#, NTF_1499PrevY#+0.05, NTF_1499PrevZ#)
				ResetEntity(Collider)
				PlayerRoom = r
				UpdateDoors()
				UpdateRooms()
				If PlayerRoom\RoomTemplate\Name = "room3storage"
					If EntityY(Collider)<-4600*RoomScale
						For i = 0 To 2
							PlayerRoom\NPC[i]\State = 2
							PositionEntity(PlayerRoom\NPC[i]\Collider, EntityX(PlayerRoom\Objects[PlayerRoom\NPC[i]\State2],True),EntityY(PlayerRoom\Objects[PlayerRoom\NPC[i]\State2],True)+0.2,EntityZ(PlayerRoom\Objects[PlayerRoom\NPC[i]\State2],True))
							ResetEntity PlayerRoom\NPC[i]\Collider
							PlayerRoom\NPC[i]\State2 = PlayerRoom\NPC[i]\State2 + 1
							If PlayerRoom\NPC[i]\State2 > PlayerRoom\NPC[i]\PrevState Then PlayerRoom\NPC[i]\State2 = (PlayerRoom\NPC[i]\PrevState-3)
						Next
					EndIf
				ElseIf PlayerRoom\RoomTemplate\Name = "pocketdimension"
					CameraFogColor Camera, 0,0,0
					CameraClsColor Camera, 0,0,0
				EndIf
				For r2.Rooms = Each Rooms
					If r2\RoomTemplate\Name = "dimension1499"
						r1499 = r2
						Exit
					EndIf
				Next
				For it.Items = Each Items
					it\disttimer = 0
					If it\itemtemplate\tempname = "scp1499" Lor it\itemtemplate\tempname = "super1499"
						If EntityY(it\collider) >= EntityY(r1499\obj)-5
							PositionEntity it\collider,NTF_1499PrevX#,NTF_1499PrevY#+(EntityY(it\collider)-EntityY(r1499\obj)),NTF_1499PrevZ#
							ResetEntity it\collider
							Exit
						EndIf
					EndIf
				Next
				r1499 = Null
				ShouldEntitiesFall = False
				PlaySound_Strict (LoadTempSound("SFX\SCP\1499\Exit.ogg"))
				NTF_1499PrevX# = 0.0
				NTF_1499PrevY# = 0.0
				NTF_1499PrevZ# = 0.0
				NTF_1499PrevRoom = Null
				Exit
			EndIf
		Next
	EndIf
End Function

Function CheckForPlayerInFacility()
	;False (=0): NPC is not in facility (mostly meant for "dimension1499")
	;True (=1): NPC is in facility
	;2: NPC is in tunnels (maintenance tunnels/049 tunnels/939 storage room, etc...)
	
	If EntityY(Collider)>100.0
		Return False
	EndIf
	If EntityY(Collider)< -10.0
		Return 2
	EndIf
	If EntityY(Collider)> 7.0 And EntityY(Collider)<=100.0
		Return 2
	EndIf
	
	Return True
End Function

Function TeleportEntity(entity%,x#,y#,z#,customradius#=0.3,isglobal%=False,pickrange#=2.0,dir%=0)
	Local pvt,pick
	;dir = 0 - towards the floor (default)
	;dir = 1 - towrads the ceiling (mostly for PD decal after leaving dimension)
	
	pvt = CreatePivot()
	PositionEntity(pvt, x,y+0.05,z,isglobal)
	If dir%=0
		RotateEntity pvt,90,0,0
	Else
		RotateEntity pvt,-90,0,0
	EndIf
	pick = EntityPick(pvt,pickrange)
	If pick<>0
		If dir%=0
			PositionEntity(entity, x,PickedY()+customradius#+0.02,z,isglobal)
		Else
			PositionEntity(entity, x,PickedY()+customradius#-0.02,z,isglobal)
		EndIf
		DebugLog "Entity teleported successfully"
	Else
		PositionEntity(entity,x,y,z,isglobal)
		DebugLog "Warning: no ground found when teleporting an entity"
	EndIf
	FreeEntity pvt
	ResetEntity entity
	DebugLog "Teleported entity to: "+EntityX(entity)+"/"+EntityY(entity)+"/"+EntityZ(entity)
End Function

Function ResetInput()
	FlushKeys()
	FlushMouse()
	MouseHit1 = 0
	MouseHit2 = 0
	MouseDown1 = 0
	MouseUp1 = 0
	MouseHit(1)
	MouseHit(2)
	MouseDown(1)
	GrabbedEntity = 0
	Input_ResetTime# = 10.0
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D