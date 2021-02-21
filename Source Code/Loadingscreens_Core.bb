Type LoadingScreens
	Field imgpath$
	Field img%
	Field ID%
	Field title$
	Field alignx%, aligny%
	Field disablebackground%
	Field txt$[5], txtamount%
End Type

Function InitLoadingScreens(file$)
	Local TemporaryString$, i%
	Local ls.LoadingScreens
	Local f = OpenFile(file)
	
	While Not Eof(f)
		TemporaryString = Trim(ReadLine(f))
		If Left(TemporaryString,1) = "[" Then
			TemporaryString = Mid(TemporaryString, 2, Len(TemporaryString) - 2)
			
			ls.LoadingScreens = New LoadingScreens
			LoadingScreenAmount=LoadingScreenAmount+1
			ls\ID = LoadingScreenAmount
			
			ls\title = TemporaryString
			;ls\imgpath = GetINIString(file, TemporaryString, "image path")
			
			For i = 0 To 4
				ls\txt[i] = GetINIString(file, TemporaryString, "text"+(i+1))
				If ls\txt[i]<> "" Then ls\txtamount=ls\txtamount+1
			Next
			
			ls\disablebackground = GetINIInt(file, TemporaryString, "disablebackground")
			
			Select Lower(GetINIString(file, TemporaryString, "align x"))
				Case "left"
					ls\alignx = -1
				Case "middle", "center"
					ls\alignx = 0
				Case "right" 
					ls\alignx = 1
			End Select 
			
			Select Lower(GetINIString(file, TemporaryString, "align y"))
				Case "top", "up"
					ls\aligny = -1
				Case "middle", "center"
					ls\aligny = 0
				Case "bottom", "down"
					ls\aligny = 1
			End Select 			
		EndIf
	Wend
	
	CloseFile f
End Function

Function DrawLoading(percent%, shortloading=False)
	Local x%, y%
	
	If percent = 100 Then loadingasset = ""
	
	If percent = 0 Then
		LoadingScreenText=0
		
		temp = Rand(1,LoadingScreenAmount)
		For ls.loadingscreens = Each LoadingScreens
			If ls\id = temp Then
				;If ls\img=0 Then ls\img = LoadImage_Strict("Loadingscreens\"+ls\imgpath)
				SelectedLoadingScreen = ls 
				Exit
			EndIf
		Next
	EndIf	
	
	firstloop = True
	Repeat 
		ClsColor 0,0,0
		Cls
		
		If percent > 20 Then
			UpdateMusic()
		EndIf
		
		If shortloading = False Then
			If percent > (100.0 / SelectedLoadingScreen\txtamount)*(LoadingScreenText+1) Then
				LoadingScreenText=LoadingScreenText+1
			EndIf
		EndIf
		
		If (Not SelectedLoadingScreen\disablebackground) Then
			DrawImage LoadingBack, GraphicWidth/2 - ImageWidth(LoadingBack)/2, GraphicHeight/2 - ImageHeight(LoadingBack)/2
		EndIf	
		
		If SelectedLoadingScreen\alignx = 0 Then
			;x = GraphicWidth/2 - ImageWidth(SelectedLoadingScreen\img)/2 
		ElseIf  SelectedLoadingScreen\alignx = 1
			;x = GraphicWidth - ImageWidth(SelectedLoadingScreen\img)
		Else
			x = 0
		EndIf
		
		If SelectedLoadingScreen\aligny = 0 Then
			;y = GraphicHeight/2 - ImageHeight(SelectedLoadingScreen\img)/2 
		ElseIf  SelectedLoadingScreen\aligny = 1
			;y = GraphicHeight - ImageHeight(SelectedLoadingScreen\img)
		Else
			y = 0
		EndIf	
		
		;DrawImage SelectedLoadingScreen\img, x, y
		
		Local width% = 300, height% = 20
		x% = GraphicWidth / 2 - width / 2
		y% = GraphicHeight / 2 + 30 - 100
		
		Rect(x, y, width+4, height, False)
		For  i% = 1 To Int((width - 2) * (percent / 100.0) / 10)
			DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
		Next
		
		Color 0,0,0
		AASetFont Font2
		;AAText(GraphicWidth / 2 + 1, GraphicHeight / 2 + 80 + 1, SelectedLoadingScreen\title, True, True)
		AAText(GraphicWidth / 2 + 1, GraphicHeight / 2 + 80 + 1, loadingasset, True, True)
		AASetFont Font1
		RowText(SelectedLoadingScreen\txt[LoadingScreenText], GraphicWidth / 2-200+1, GraphicHeight / 2 +120+1,400,300,True)
			
		Color 255,255,255
		AASetFont Font2
		;AAText(GraphicWidth / 2, GraphicHeight / 2 +80, SelectedLoadingScreen\title, True, True)
		AAText(GraphicWidth / 2, GraphicHeight / 2 +80, loadingasset, True, True)
		AASetFont Font1
		RowText(SelectedLoadingScreen\txt[LoadingScreenText], GraphicWidth / 2-200, GraphicHeight / 2 +120,400,300,True)
		
		Color 0,0,0
		AAText(GraphicWidth / 2 + 1, GraphicHeight / 2 - 100 + 1, "LOADING - " + percent + " %", True, True)
		Color 255,255,255
		AAText(GraphicWidth / 2, GraphicHeight / 2 - 100, "LOADING - " + percent + " %", True, True)
		
		If percent = 100 Then 
			AAText(GraphicWidth / 2, GraphicHeight - 50, "PRESS ANY KEY TO CONTINUE", True, True)
		Else
			FlushKeys()
			FlushMouse()
		EndIf
		
		UpdateScreenGamma()
		
		Flip False
		
		firstloop = False
		If percent <> 100 Then Exit
		
	Until (GetKey()<>0 Lor MouseHit(1))
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D