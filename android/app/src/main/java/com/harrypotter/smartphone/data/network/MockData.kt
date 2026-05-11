package com.harrypotter.smartphone.data.network

import com.harrypotter.smartphone.data.model.*

object MockData {
    val dlcs = listOf(
        DLCSummary(
            dlcId = "sirius_must_live",
            title = "Sirius Black Must Live",
            description = "The Department of Mysteries was a trap. Can you arrive in time to prevent the fatal curse and save Harry's godfather?",
            maxScore = 100
        ),
        DLCSummary(
            dlcId = "remus_must_survive",
            title = "Remus Must Survive",
            description = "The Battle of Hogwarts claims many lives. Stand with the Lupins to change the course of history.",
            maxScore = 100
        ),
        DLCSummary(
            dlcId = "fred_must_live",
            title = "Fred Must Live",
            description = "A wall collapses, a laugh is cut short. Intervene in the corridor to keep the Weasley twins together.",
            maxScore = 100
        ),
        DLCSummary(
            dlcId = "cedric_must_win",
            title = "Cedric Must Win",
            description = "The Triwizard Cup is a Portkey to tragedy. Can you help the spare survive the graveyard?",
            maxScore = 100
        ),
        DLCSummary(
            dlcId = "dumbledore_not_fall",
            title = "Dumbledore Must Not Fall",
            description = "The Astronomy Tower, the Lightning-Struck Tower. Find another way for the Headmaster and Draco.",
            maxScore = 100
        )
    )
}
