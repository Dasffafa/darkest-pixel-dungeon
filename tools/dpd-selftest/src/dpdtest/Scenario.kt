package dpdtest

interface Scenario {
    fun describe(): String
    fun run(h: H)
}
