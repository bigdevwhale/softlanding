package app.softlanding

import app.softlanding.data.Codes
import app.softlanding.data.Entry
import app.softlanding.data.Food
import app.softlanding.data.Meal
import app.softlanding.data.SharedDish
import app.softlanding.data.Source
import app.softlanding.domain.Activity
import app.softlanding.domain.Amount
import app.softlanding.domain.Journey
import app.softlanding.domain.Label
import app.softlanding.domain.Landing
import app.softlanding.domain.Norm
import app.softlanding.domain.Profile
import app.softlanding.domain.QuickLine
import app.softlanding.domain.Search
import app.softlanding.domain.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private val profile = Profile(Sex.F, 32, 168, 72.0, 64.0, Activity.LIGHT, .5)

    @Test fun normIsMifflinMinusDeficit() {
        val n = Norm.of(profile)
        // BMR 10·72 + 6.25·168 − 5·32 − 161 = 1449; ×1.375 = 1992; −550 = 1442.
        assertEquals(1449, n.bmr)
        assertEquals(1442, n.kcal)
        assertTrue(n.lo < n.kcal && n.hi > n.kcal)
        assertEquals(115, n.protein)
    }

    @Test fun normNeverBelowFloor() {
        val n = Norm.of(profile.copy(weight = 45.0, height = 150, age = 70, activity = Activity.SITTING, pace = .75, goal = 40.0))
        assertEquals(1200, n.kcal)
    }

    @Test fun manualNormWins() = assertEquals(1800, Norm.of(profile, 1800).kcal)

    private fun e(day: Long, kcal: Double) = Entry(day = day, meal = Meal.LUNCH, at = 0, name = "x", grams = 100.0, kcal = kcal, p = 120.0)

    @Test fun landingsReplay() {
        val n = Norm.of(profile)
        val es = listOf(e(10, n.kcal.toDouble()), e(11, n.hi + 300.0), e(12, n.lo - 400.0), e(14, n.kcal.toDouble()), e(15, 5000.0))
        val j = Journey.replay(es, setOf(15L), emptyList(), profile, 0, 10, 16)
        assertEquals(listOf(Landing.SOFT, Landing.EARLY, Landing.DRIFT, Landing.FOG, Landing.SOFT, Landing.FAIR), j.flights.map { it.landing })
        assertEquals(2, j.soft)
        // Fog breaks the days-with-records streak; the fair keeps the soft run going.
        assertEquals(2, j.streak)
        assertEquals(2, j.softStreak)
        assertEquals(1, j.flights[4].at)
    }

    @Test fun quickLine() {
        val bits = QuickLine.parse("гречка 200, 2 яйца и кофе; 300 ккал")
        assertEquals(4, bits.size)
        assertEquals("гречка", bits[0].text); assertEquals(200.0, bits[0].amount); assertEquals(Amount.GRAMS, bits[0].unit)
        assertEquals("яйца", bits[1].text); assertEquals(Amount.PIECES, bits[1].unit)
        assertEquals("кофе", bits[2].text); assertNull(bits[2].amount)
        assertEquals(Amount.KCAL, bits[3].unit); assertEquals(300.0, bits[3].amount)
        assertEquals(1500.0, QuickLine.parse("молоко 1.5 л")[0].amount)
    }

    @Test fun searchFindsInflected() {
        val foods = listOf(
            Food(id = 1, name = "Гречка варёная", en = "Buckwheat, cooked", kcal = 110.0, p = 4.0, f = 1.0, c = 21.0, source = Source.BUILTIN),
            Food(id = 2, name = "Яйцо куриное", en = "Egg", kcal = 157.0, p = 13.0, f = 11.0, c = 1.0, aliases = "яйца,яичко", source = Source.BUILTIN),
            Food(id = 3, name = "Яблоко", kcal = 47.0, p = 0.0, f = 0.0, c = 10.0, source = Source.BUILTIN),
        )
        assertEquals(1L, Search.find(foods, "гречку", 0).first().id)
        assertEquals(2L, Search.find(foods, "яйца", 0).first().id)
        assertEquals(2L, Search.find(foods, "egg", 0).first().id)
        assertTrue(Search.find(foods, "банан", 0).isEmpty())
    }

    @Test fun searchRealTablePrefersPlateForms() {
        val foods = app.softlanding.data.Builtin.parse(java.io.File("src/main/assets/foods.tsv").readLines()).mapIndexed { i, f -> f.copy(id = i + 1L) }
        fun top(q: String) = Search.find(foods, q, 0).first().key
        assertEquals("buckwheat_c", top("buckwheat"))
        assertEquals("buckwheat_c", top("гречка"))
        assertEquals("buckwheat_d", top("гречка сухая"))
        assertEquals("egg", top("egg"))
        assertEquals("egg", top("яйцо"))
        assertEquals("apple", top("яблоко"))
        assertEquals("milk_25", top("молоко"))
        assertEquals("cappuccino", top("капучино"))
    }

    @Test fun labelRussianMangled() {
        // What the Latin recogniser makes of a Russian table: the numbers survive, the words don't.
        val t = "Пищевая ценность в 100 г: 6enkn 3,0 r, xnpbI 3,2 r, yrnebodbI 4,7 r. Энергетическая ценность 250 кДж / 60 KKan"
        val g = Label.read(t)
        assertEquals(60.0, g.kcal!!, .01)
        assertEquals(3.0, g.p!!, .01); assertEquals(3.2, g.f!!, .01); assertEquals(4.7, g.c!!, .01)
    }

    @Test fun labelEnglish() {
        val t = "Nutrition per 100g\nEnergy 1046kJ / 250kcal\nFat 9.0g\nof which saturates 5.1g\nCarbohydrate 33g\nof which sugars 20g\nProtein 8.5g\nSalt 0.4g"
        val g = Label.read(t)
        assertEquals(250.0, g.kcal!!, .01); assertEquals(9.0, g.f!!, .01); assertEquals(33.0, g.c!!, .01); assertEquals(8.5, g.p!!, .01)
    }

    @Test fun codes() {
        assertEquals("4607001234567", Codes.gtin("4607001234567"))
        // «Честный знак»: 01 + GTIN-14, then serial and crypto tail.
        assertEquals("4607001234567", Codes.gtin("0104607001234567215Abc\u001d93dGVz"))
        assertNull(Codes.gtin("https://example.com"))
    }

    @Test fun sharedDishRoundTrip() {
        val d = Food(name = "Борщ «как у бабушки»", kcal = 52.3, p = 2.1, f = 2.5, c = 5.4, cooked = 3200.0, servings = 8, source = Source.DISH)
        val back = SharedDish.decode(SharedDish.encode(d))
        assertNotNull(back)
        assertEquals(d.name, back!!.name)
        assertEquals(3200.0, back.cooked, .01)
        assertEquals(8, back.servings)
    }
}
