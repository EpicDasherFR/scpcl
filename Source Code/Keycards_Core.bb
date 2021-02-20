Type Keycard
	Field permcc1% = 0
	Field permcc2% = 0
	Field permcc3% = 0
	
	Field perma1% = 0
	Field perma2% = 0
	Field perma3% = 0
	
	Field permcheckp% = 0
	
	Field perminterc% = 0
	
	Field permexit% = 0
	
	Field permnuke% = 0
End Type

Function GetKeycardPerms()
	Local key.Keycard
	Local temp% = 0
	Select SelectedItem\itemtemplate\tempname
		Case "keyjanitor"
			key\permcc1 = 1
		Case "keyscientist"
			key\permcc1 = 1
			key\permcc2 = 1
		Case "keysupervisor"
			key\permcc1 = 1
			key\permcc2 = 1
			key\permcheckp = 1
		Case "keyengineer"
			key\permcc1 = 1
			key\permcc2 = 1
			key\permcc3 = 1
			key\permcheckp = 1
			key\perminterc = 1
			key\permnuke = 1
		Case "keyguard"
			key\permcc1 = 1
			key\perma1 = 1
			key\permcheckp = 1
		Case "keycadet"
			key\permcc1 = 1
			key\permcc2 = 1
			key\perma1 = 1
			key\perma2 = 1
			key\permcheckp = 1
		Case "keylieutenant"
			key\permcc1 = 1
			key\permcc2 = 1
			key\perma1 = 1
			key\perma2 = 1
			key\permcheckp = 1
			key\permexit = 1
		Case "keycommander"
			key\permcc1 = 1
			key\permcc2 = 1
			key\perma1 = 1
			key\perma2 = 1
			key\perma3 = 1
			key\permcheckp = 1
			key\perminterc = 1
			key\permexit = 1
		Case "keyzmanager"
			key\permcc1 = 1
			key\permcheckp = 1
		Case "keyfmanager"
			key\permcc1 = 1
			key\permcc2 = 1
			key\permcc3 = 1
			key\permcheckp = 1
			key\perminterc = 1
			key\permexit = 1
			key\permnuke = 1
		Case "keychaos"
			key\permcc1 = 1
			key\permcc2 = 1
			key\perma1 = 1
			key\perma2 = 1
			key\perma3 = 1
			key\permcheckp = 1
			key\perminterc = 1
			key\permexit = 1
		Case "keyO5"
			key\permcc1 = 1
			key\permcc2 = 1
			key\permcc3 = 1
			key\perma1 = 1
			key\perma2 = 1
			key\perma3 = 1
			key\permcheckp = 1
			key\perminterc = 1
			key\permexit = 1
			key\permnuke = 1
	End Select
End Function
			
			
			
			
			
			
			
			
;~IDEal Editor Parameters:
;~C#Blitz3D