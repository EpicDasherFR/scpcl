; // New clearance system made by EpicDasherFR for Containment Laboratory mod.
; Each type of permission (containment chamber tier 2, nuke access, armory tier 3...) has a value, for each keycard, I set the values to the corresponding perms of the card.
; When a card is used on the keycard reader, the games set the perms values to the equipped keycard, if the door needed clearance is present, the door opens.
Global key_permcc1%
Global key_permcc2%
Global key_permcc3%

Global key_perma1%
Global key_perma2%
Global key_perma3%

Global key_permcheckp%
Global key_perminterc%
Global key_permexit%
Global key_permnuke%

Global key_nokey%

Function GetKeycardPerms()
	key_permcc1% = 0
	key_permcc2% = 0
	key_permcc3% = 0
	
	key_perma1% = 0
	key_perma2% = 0
	key_perma3% = 0
	
	key_permcheckp% = 0
	key_perminterc% = 0
	key_permexit% = 0
	key_permnuke% = 0
	
	key_nokey% = 0
	
	Select SelectedItem\itemtemplate\tempname
		Case "keyjanitor"
			key_permcc1% = 1
		Case "keyscientist"
			key_permcc1% = 1
			key_permcc2% = 1
		Case "keysupervisor"
			key_permcc1% = 1
			key_permcc2% = 1
			key_permcheckp% = 1
		Case "keyengineer"
			key_permcc1% = 1
			key_permcc2% = 1
			key_permcc3% = 1
			key_permcheckp% = 1
			key_perminterc% = 1
			key_permnuke% = 1
		Case "keyguard"
			key_permcc1% = 1
			key_perma1% = 1
			key_permcheckp% = 1
		Case "keycadet"
			key_permcc1% = 1
			key_permcc2% = 1
			key_perma1% = 1
			key_perma2% = 1
			key_permcheckp% = 1
		Case "keylieutenant"
			key_permcc1% = 1
			key_permcc2% = 1
			key_perma1% = 1
			key_perma2% = 1
			key_permcheckp% = 1
			key_permexit% = 1
		Case "keycommander"
			key_permcc1% = 1
			key_permcc2% = 1
			key_perma1% = 1
			key_perma2% = 1
			key_perma3% = 1
			key_permcheckp% = 1
			key_perminterc% = 1
			key_permexit% = 1
		Case "keyzmanager"
			key_permcc1% = 1
			key_permcheckp% = 1
		Case "keyfmanager"
			key_permcc1% = 1
			key_permcc2% = 1
			key_permcc3% = 1
			key_permcheckp% = 1
			key_perminterc% = 1
			key_permexit% = 1
			key_permnuke% = 1
		Case "keychaos"
			key_permcc1% = 1
			key_permcc2% = 1
			key_perma1% = 1
			key_perma2% = 1
			key_perma3% = 1
			key_permcheckp% = 1
			key_perminterc% = 1
			key_permexit% = 1
		Case "keyO5"
			key_permcc1% = 1
			key_permcc2% = 1
			key_permcc3% = 1
			key_perma1% = 1
			key_perma2% = 1
			key_perma3% = 1
			key_permcheckp% = 1
			key_perminterc% = 1
			key_permexit% = 1
			key_permnuke% = 1
		Default
			key_nokey% = 1
	End Select
End Function
			
			
			
;~IDEal Editor Parameters:
;~C#Blitz3D