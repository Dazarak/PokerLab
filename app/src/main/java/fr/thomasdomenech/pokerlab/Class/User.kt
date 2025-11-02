package fr.thomasdomenech.pokerlab.Class

import fr.thomasdomenech.pokerlab.Model.Card

class User {
    var name: String = ""
    var money: Int = 0
    var bet: Int = 0
    var action: String = ""
    var turn: Int = 0
    var card1: Card? = null
    var card2: Card? = null
}