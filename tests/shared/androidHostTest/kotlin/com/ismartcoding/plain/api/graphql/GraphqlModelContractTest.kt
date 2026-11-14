package com.ismartcoding.plain.api.graphql

import com.ismartcoding.plain.models.Share
import com.ismartcoding.plain.models.ShareInput
import com.ismartcoding.plain.models.SharePath

import com.ismartcoding.plain.models.SmsAttachment

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import com.ismartcoding.plain.models.ActionResult
import com.ismartcoding.plain.models.App
import com.ismartcoding.plain.models.AppFile
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.AudioFileInfo
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioPlayback
import com.ismartcoding.plain.models.AudioPlaylist
import com.ismartcoding.plain.models.Call
import com.ismartcoding.plain.models.Certificate
import com.ismartcoding.plain.models.ChatChannel
import com.ismartcoding.plain.models.ChatChannelMember
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.features.chat.models.MessageDeliveryResult
import com.ismartcoding.plain.models.ClipboardItem
import com.ismartcoding.plain.models.Contact
import com.ismartcoding.plain.models.ContactAddress
import com.ismartcoding.plain.models.ContactAddressInput
import com.ismartcoding.plain.models.ContactEmail
import com.ismartcoding.plain.models.ContactEmailInput
import com.ismartcoding.plain.models.ContactEvent
import com.ismartcoding.plain.models.ContactEventInput
import com.ismartcoding.plain.models.ContactGroup
import com.ismartcoding.plain.models.ContactIm
import com.ismartcoding.plain.models.ContactImInput
import com.ismartcoding.plain.models.ContactInput
import com.ismartcoding.plain.models.ContactPhoneInput
import com.ismartcoding.plain.models.ContactPhoneNumber
import com.ismartcoding.plain.models.ContactWebsite
import com.ismartcoding.plain.models.ContactWebsiteInput
import com.ismartcoding.plain.models.Doc
import com.ismartcoding.plain.models.FavoriteFolder
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.FeedEntryCount
import com.ismartcoding.plain.models.FeedError
import com.ismartcoding.plain.models.FeedSyncState
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.models.Image
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.models.ImageEditorProjectInput
import com.ismartcoding.plain.models.ImageEditorProjectSummary
import com.ismartcoding.plain.models.ImageFileInfo
import com.ismartcoding.plain.models.Location
import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.models.NoteInput
import com.ismartcoding.plain.models.Notification
import com.ismartcoding.plain.models.ContactOrganization
import com.ismartcoding.plain.models.ContactOrganizationInput
import com.ismartcoding.plain.models.Package
import com.ismartcoding.plain.models.PairPeerInput
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PhoneGeo
import com.ismartcoding.plain.models.PomodoroSettings
import com.ismartcoding.plain.models.PomodoroToday
import com.ismartcoding.plain.models.ScreenMirrorQuality
import com.ismartcoding.plain.models.ScreenMirrorVideoCodec
import com.ismartcoding.plain.models.Sim
import com.ismartcoding.plain.models.Sms
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.models.VideoFileInfo
import kotlinx.serialization.ExperimentalSerializationApi

class GraphqlModelContractTest {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun baseModelsMatchSchemaWithoutLazyTagFields() {
        val sdl = readMainSchema()
            .replace(Regex("\"\"\"[\\s\\S]*?\"\"\""), "")
        val models: Map<String, SerialDescriptor> = mapOf(
            "ActionResult" to ActionResult.serializer().descriptor,
            "App" to App.serializer().descriptor,
            "AppFile" to AppFile.serializer().descriptor,
            "Audio" to Audio.serializer().descriptor,
            "AudioFileInfo" to AudioFileInfo.serializer().descriptor,
            "AudioItem" to AudioItem.serializer().descriptor,
            "AudioPlayback" to AudioPlayback.serializer().descriptor,
            "AudioPlaylist" to AudioPlaylist.serializer().descriptor,
            "Call" to Call.serializer().descriptor,
            "Certificate" to Certificate.serializer().descriptor,
            "ChatChannel" to ChatChannel.serializer().descriptor,
            "ChatChannelMember" to ChatChannelMember.serializer().descriptor,
            "ChatItem" to ChatItem.serializer().descriptor,
            "ChatDeliveryResult" to MessageDeliveryResult.serializer().descriptor,
            "ClipboardItem" to ClipboardItem.serializer().descriptor,
            "Contact" to Contact.serializer().descriptor,
            "ContactAddress" to ContactAddress.serializer().descriptor,
            "ContactAddressInput" to ContactAddressInput.serializer().descriptor,
            "ContactEmail" to ContactEmail.serializer().descriptor,
            "ContactEmailInput" to ContactEmailInput.serializer().descriptor,
            "ContactEvent" to ContactEvent.serializer().descriptor,
            "ContactEventInput" to ContactEventInput.serializer().descriptor,
            "ContactGroup" to ContactGroup.serializer().descriptor,
            "ContactIm" to ContactIm.serializer().descriptor,
            "ContactImInput" to ContactImInput.serializer().descriptor,
            "ContactInput" to ContactInput.serializer().descriptor,
            "ContactPhoneInput" to ContactPhoneInput.serializer().descriptor,
            "ContactPhoneNumber" to ContactPhoneNumber.serializer().descriptor,
            "ContactWebsite" to ContactWebsite.serializer().descriptor,
            "ContactWebsiteInput" to ContactWebsiteInput.serializer().descriptor,
            "Doc" to Doc.serializer().descriptor,
            "FavoriteFolder" to FavoriteFolder.serializer().descriptor,
            "Feed" to Feed.serializer().descriptor,
            "FeedEntry" to FeedEntry.serializer().descriptor,
            "FeedEntryCount" to FeedEntryCount.serializer().descriptor,
            "FeedError" to FeedError.serializer().descriptor,
            "FeedSyncState" to FeedSyncState.serializer().descriptor,
            "File" to File.serializer().descriptor,
            "Share" to Share.serializer().descriptor,
            "ShareInput" to ShareInput.serializer().descriptor,
            "SharePath" to SharePath.serializer().descriptor,
            "SharePathInput" to SharePath.serializer().descriptor,
            "Image" to Image.serializer().descriptor,
            "ImageEditorProject" to ImageEditorProject.serializer().descriptor,
            "ImageEditorProjectInput" to ImageEditorProjectInput.serializer().descriptor,
            "ImageEditorProjectSummary" to ImageEditorProjectSummary.serializer().descriptor,
            "ImageFileInfo" to ImageFileInfo.serializer().descriptor,
            "Location" to Location.serializer().descriptor,
            "MediaBucket" to MediaBucket.serializer().descriptor,
            "Note" to Note.serializer().descriptor,
            "NoteInput" to NoteInput.serializer().descriptor,
            "Notification" to Notification.serializer().descriptor,
            "ContactOrganization" to ContactOrganization.serializer().descriptor,
            "ContactOrganizationInput" to ContactOrganizationInput.serializer().descriptor,
            "Package" to Package.serializer().descriptor,
            "PairPeerInput" to PairPeerInput.serializer().descriptor,
            "Peer" to Peer.serializer().descriptor,
            "PhoneGeo" to PhoneGeo.serializer().descriptor,
            "PomodoroSettings" to PomodoroSettings.serializer().descriptor,
            "PomodoroToday" to PomodoroToday.serializer().descriptor,
            "ScreenMirrorQuality" to ScreenMirrorQuality.serializer().descriptor,
            "ScreenMirrorVideoCodec" to ScreenMirrorVideoCodec.serializer().descriptor,
            "Sim" to Sim.serializer().descriptor,
            "Sms" to Sms.serializer().descriptor,
            "SmsAttachment" to SmsAttachment.serializer().descriptor,
            "Tag" to Tag.serializer().descriptor,
            "TagRelation" to TagRelation.serializer().descriptor,

            "Video" to Video.serializer().descriptor,
            "VideoFileInfo" to VideoFileInfo.serializer().descriptor,
        )
        val schemaWithoutDescriptions = Regex("\"\"\"[\\s\\S]*?\"\"\"").replace(sdl, "")
        val blocks = Regex("""(?:type|input) (\w+)(?:\s+implements\s+\w+)?(?:\s+@oneOf)?\s*\{([^}]+)}""").findAll(schemaWithoutDescriptions)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val lazyTags = setOf("Audio", "Call", "Contact", "Sms", "Doc", "Image", "Video", "Note", "FeedEntry")
        val mismatches = mutableListOf<String>()
        models.forEach { (name, descriptor) ->
            val fields = Regex("""(\w+)\s*:\s*([\w!\[\]]+)""").findAll(blocks.getValue(name))
                .associate { it.groupValues[1] to !it.groupValues[2].endsWith("!") }
            val actual = (0 until descriptor.elementsCount).associate {
                descriptor.getElementName(it) to descriptor.getElementDescriptor(it).isNullable
            }
            if (name in lazyTags) {
                assertEquals(false, fields.getValue("tags"), "$name.tags remains a non-null GraphQL field")
                assertEquals(false, actual.containsKey("tags"), "$name.tags must be resolved separately")
            }
            if (name == "ChatChannel") {
                assertEquals(false, fields.getValue("key"), "The trusted native key remains in the schema")
                assertEquals(false, actual.containsKey("key"), "The UI model must not hydrate the channel key")
            }
            val expected = fields.filterKeys { (it != "tags" || name !in lazyTags) && (name != "ChatChannel" || it != "key") }
            if (expected != actual) mismatches.add("$name expected=$expected actual=$actual")
        }
        assertEquals(emptyList<String>(), mismatches)
    }
}
