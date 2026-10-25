package com.kevinwcode.personalapi

import java.time.LocalDate

data class Observation(val date: LocalDate, val metric: String, val value: Double, val unit: String)
