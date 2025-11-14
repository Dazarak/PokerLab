package fr.thomasdomenech.pokerlab.Model

data class Action(
    var Check: Double = 0.0,
    var Call: Double = 0.0,
    var Fold: Double = 0.0,
    var Raise: Double = 0.0,
    var RaisePourcent : Int = 2
)
