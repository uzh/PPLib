package ch.uzh.ifi.pdeboer.pplib.patterns

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 28/11/14.
 */
class SigmaPrunerTest {
	val MEAN: Int = 10
	val STDDEV: Int = 2

	@Test
	def meanCalculatedCorrectly: Unit = {
		Assert.assertEquals(MEAN, sp.mean, .3) //.3 tolerance for epsilon
	}

	@Test
	def stddevCalculatedCorrectly: Unit = {
		Assert.assertEquals(STDDEV, sp.stddev, .3)
	}

	@Test
	def testMinAndMaxAllowedValue = {
		Assert.assertEquals(MEAN - 3 * STDDEV, sp.minAllowedValue, 0.3)
		Assert.assertEquals(MEAN + 3 * STDDEV, sp.maxAllowedValue, 0.3)
	}

	@Test
	def testRange: Unit = {
		val reducedDS: Set[Double] = dataset.filter(_ >= MEAN - 3 * STDDEV).filter(_ < MEAN + 3 * STDDEV).toSet
		Assert.assertEquals(reducedDS, sp.dataWithinRange.toSet)
	}

	def sp = new SigmaPruner(dataset)

	//generated by http://www.random.org/gaussian-distributions/?num=100&mean=10&stdev=2&dec=10&col=1&notation=scientific&format=html&rnd=new
	val dataset: List[Double] = List(7.928252127, 15.38017731, 10.32705409, 12.40635447, 12.1885995, 10.88501599, 13.43200765, 12.09382078, 10.9597842, 9.375771673, 7.799507983, 13.19055072, 7.164326685, 10.45501007, 7.708375853, 12.97525099, 9.263183387, 9.368352096, 9.858230929, 7.709063386, 9.39757451, 9.581381768, 5.994090751, 12.03011004, 9.301240406, 12.43418488, 10.42236757, 11.10148666, 10.08950135, 12.91391816, 11.82085222, 12.20885789, 12.31014714, 11.74971723, 9.01040776, 9.963946789, 10.23974189, 10.2362398, 7.232119932, 8.954302003, 9.409944641, 8.965526776, 6.421997802, 9.198235961, 7.424924286, 12.16508684, 9.503328832, 11.96334651, 7.996725938, 7.926184579, 10.87916748, 7.411559968, 9.863867797, 11.37324527, 12.18146089, 8.743770669, 12.93617991, 10.03105243, 10.92488199, 10.27285988, 9.013438095, 11.2256489, 10.06865673, 10.219797, 11.67828247, 7.300658657, 7.898444513, 5.788228243, 9.203732309, 9.124837655, 7.503493793, 9.832497959, 13.15697326, 10.3527046, 8.354746585, 9.114502311, 9.342949781, 9.879510235, 6.407604287, 9.197239834, 9.096656238, 9.535565918, 10.71456916, 8.366788073, 11.8495085, 10.20562197, 12.95247165, 10.31715824, 10.41397438, 11.69013755, 9.923362345, 13.75528711, 10.017165, 11.12824386, 9.678515344, 11.7935328, 11.84009805, 12.89547596, 12.08283953, 9.07774799, 11.22926141, 7.782836441, 8.899788413, 10.54209733, 12.99872782, 11.41981719, 13.86780582, 8.530223285, 7.698795406, 12.67027785, 14.4926609, 10.03909811, 11.51466493, 8.508356868, 13.43987055, 8.980615589, 9.401866244, 7.683429906, 10.0479483, 11.12444732, 11.78882454, 10.24918739, 9.912035382, 8.010315043, 9.7716339, 12.8257215, 9.504529626, 8.431707728, 9.556048232, 7.260579592, 12.22547574, 11.98065559, 10.85363784, 13.44857929, 11.93618833, 10.03509912, 9.360644423, 14.73951119, 10.22540542, 7.36897656, 11.02316359, 5.711477519, 8.856349449, 10.01479799, 9.904744112, 10.70935918, 7.669676552, 10.96977961, 8.595272778, 7.451246889, 12.20857144, 12.04631379, 11.68829979, 6.426050902, 7.465771478, 9.38433219, 10.177597, 9.873351362, 10.03209306, 10.99200418, 8.462313889, 8.460143226, 11.25218606, 10.66038443, 9.812357943, 10.66027913, 7.8043289, 9.750786958, 8.618752385, 7.377667543, 9.473607742, 7.35653159, 6.315633325, 11.02923606, 9.376458935, 10.90040066, 7.90189181, 9.515738594, 8.107966829, 11.3114617, 10.43573489, 10.78212544, 8.778963354, 5.489913226, 9.856231975, 10.06888072, 11.0582867, 8.655980824, 10.01409947, 12.6137975, 13.59914713, 11.63288769, 6.479550214, 10.69596382, 8.689567558, 11.00498335, 9.387312889, 10.84602249, 9.601907628, 10.63650398, 10.23631827, 12.37459362, 6.556433431, 10.71684665, 10.49075469, 11.08071999, 10.31154508, 10.16898481, 10.76613412, 11.07150395, 5.559480757, 9.852187402, 12.28952854, 10.89358857, 5.253132599, 6.761306054, 9.789371995, 9.565452725, 7.888540689, 9.619630132, 8.1150553, 10.80195923, 13.3234374, 9.889035611, 11.67993637, 7.439140235, 12.03856343, 9.639867449, 8.79489293, 10.73582429, 11.45992362, 9.753465223, 8.134438873, 7.71306436, 11.3471607, 13.3972776, 13.04868214, 9.404019124, 7.794110548, 9.878822777, 8.541747912, 8.998691298, 7.416561384, 10.56443377, 9.405831469, 11.22180362, 7.316644455, 8.46832098, 9.652666416, 7.739145257, 12.91433682, 9.921551568, 7.810969613, 6.170072886, 8.879200955, 8.199700302, 14.30808776, 10.77562188, 11.46191158, 13.72312139, 10.02840306, 9.09031321, 7.931980169, 9.848908913, 7.676583993, 10.09178958, 7.387686219, 7.305075088, 11.2498461, 9.970938788, 10.13676457, 8.038912776, 7.434393698, 9.788947233, 11.54661964, 9.086911832, 13.69019806, 6.571436901, 9.157313016, 6.193771624, 8.516928891, 9.585920553, 6.961516572, 6.850048074, 7.528874174, 9.70240163, 10.01501888, 8.070972673, 10.09957167, 10.94627093, 11.32429416, 8.54104213, 8.972930656, 8.052895199, 10.53865448, 10.31885743, 7.715011188, 11.70376489, 7.833425474, 9.321588214, 10.40336903, 8.650984933, 7.694106894, 11.39544898, 11.19779541, 8.641659354, 9.505027726, 11.58485114, 11.41240237, 10.93954221, 10.36918529, 10.19661303, 12.75677142, 9.364250701, 12.61346289, 9.817218099, 12.06483955, 9.30811109, 10.89008503, 8.828840785, 9.516346798, 9.194071034, 10.25922708, 9.576754433, 7.413552793, 7.209918547, 11.55128092, 9.775480668, 7.211541358, 8.757733365, 8.437228881, 11.42755515, 9.122756458, 7.90963252, 11.05428543, 9.949616224, 8.566120635, 12.96859828, 12.24535383, 10.7997834, 12.09532389, 8.372497348, 6.208747163, 9.352361488, 11.61200966, 7.15651394, 8.398994829, 14.23372434, 10.60820797, 8.741486196, 4.698409458, 8.410336943, 12.57059077, 9.833673568, 10.95135649, 9.142179431, 8.570535441, 7.725370994, 11.12319174, 7.919962373, 6.975189183, 14.2583245, 9.630999461, 7.149931508, 12.29366376, 12.87727193, 11.64281947, 9.231862242, 11.45648873, 8.784757765, 14.73847404, 10.49775456, 8.819594778, 11.97489345, 11.30441369, 10.5432425, 7.359442816, 4.801207683, 8.882651104, 7.701340762, 8.878178875, 13.82578311, 4.391417266, 12.80525151, 15.11938477, 12.30042352, 10.95397013, 8.947049754, 13.13950434, 11.62155182, 8.366439646, 7.374492869, 11.83344969, 10.33624182, 11.61129305, 3.525188355, 9.120388412, 12.04390612, 14.42337329, 11.69628511, 9.875599565, 9.991005071, 11.15241186, 6.371452569, 9.202152766, 10.14828041, 10.12782036, 9.536956746, 8.985622162, 7.957262815, 10.8218048, 7.632083076, 9.139123863, 13.25154221, 12.58623188, 9.696147931, 9.444432433, 14.40011801, 10.13550904, 8.600716418, 8.177245894, 8.669697787, 15.03662591, 6.889206303, 12.04897024, 10.9013879, 11.25983931, 7.398636922, 7.18264093, 8.754478191, 9.902627562, 8.471257397, 7.809232417, 5.34402346, 9.827955558, 10.78647697, 14.48412558, 9.026643185, 10.6756742, 11.03037461, 8.268612685, 9.88670982, 10.24040639, 13.31884178, 7.790264313, 14.56809884, 10.01308531, 8.695737656, 11.36082418, 11.04624104, 9.193983013, 9.274135118, 7.476376763, 15.12256453, 9.025771181, 11.41656532, 10.79218922, 9.455242219, 14.64272775, 8.555701937, 11.48733378, 11.1329715, 10.04012241, 8.009189142, 10.72094666, 11.43007142, 11.91692622, 6.81442819, 9.099668102, 12.94850289, 11.22964792, 9.831803795, 12.94152082, 10.66122886, 6.99304154, 12.90336608, 7.259769487, 12.72385462, 12.34789021, 12.57247703, 8.99012059, 11.93414943, 13.62920204, 7.812957179, 7.083493122, 7.71113265, 9.578915293, 9.246222294, 10.28466237, 11.06318796, 9.143414481, 11.46296168, 11.72437598, 9.551531927, 10.03357732, 9.987461842, 11.32460365, 8.023883863, 10.92515256, 9.11332777, 13.15040099, 8.848769942, 11.25548853, 10.32249277, 10.37913157, 6.281906175, 8.283677016, 7.881663282, 9.164993135, 10.12427068, 11.7315942, 8.308240593, 14.00335568, 10.50947937, 10.02645037, 11.37873274, 9.155379912, 11.00808773, 9.350540631, 8.64208427, 9.479417295, 10.33793486, 9.132450131, 7.289269383, 11.13487894, 10.06544903, 9.044094811, 7.844049218, 7.475676182, 8.459984278, 13.66273477, 11.65447682, 10.4711732, 10.12387804, 8.077052682, 13.15583922, 8.425122706, 11.59561794, 10.00729191, 10.08096314, 8.749150131, 9.50382059, 9.119121783, 8.635105199, 6.758608213, 10.23707035, 10.273075, 12.33814968, 8.559455475, 9.702088964, 12.70556224, 11.35063683, 10.96658356, 6.937588096, 7.287710403, 10.91656489, 11.02671799, 10.01438267, 11.46516754, 12.22912153, 9.194727939, 10.20789498, 10.57337165, 8.587825927, 9.656585362, 8.871644624, 13.945056, 9.377297082, 9.366246239, 9.316218799, 8.582665435, 7.081874131, 14.33471279, 10.24973761, 13.44986648, 11.76335822, 6.80669987, 10.18307259, 13.35306268, 10.59119244, 10.54659397, 11.800118, 13.5777512, 11.36932437, 10.0115348, 8.611856953, 7.433249934, 11.59750071, 9.949627745, 12.07753776, 10.60821747, 5.808532723, 6.916067104, 7.462076559, 8.346776591, 7.47735643, 8.944770144, 11.90740283, 7.675259434, 12.88187857, 11.31964405, 12.90549642, 9.18484506, 6.67648841, 10.8867671, 6.474035503, 11.91599314, 10.73347031, 10.95697314, 12.8623418, 13.97085383, 10.21427968, 8.495780336, 9.725637257, 7.579183211, 13.13548544, 8.559095823, 13.30007132, 7.710523482, 13.7970617, 10.93183905, 9.745149312, 11.56328997, 8.963731192, 6.298060792, 10.88655431, 12.37954878, 9.584219572, 7.391228453, 11.82565544, 7.686697148, 10.99765766, 11.73072144, 8.597838703, 8.692086247, 10.67032099, 7.562779975, 9.445026492, 10.96055687, 7.661682905, 8.834611677, 11.39318699, 11.80737792, 7.004773484, 10.80685847, 8.950667322, 11.9335431, 7.660813633, 9.194467521, 8.38358654, 10.77423585, 11.28506842, 9.114933574, 10.88726405, 11.52501321, 9.33629586, 7.667290126, 10.01265552, 9.175774499, 10.12148551, 10.51910091, 9.960732042, 8.613087973, 9.229355332, 9.753502107, 9.839965226, 11.79652052, 7.495869843, 9.516733108, 8.320191717, 11.20781279, 8.663387863, 7.304475565, 11.33844555, 8.991102017, 13.00481197, 11.24899306, 8.632656568, 11.48730924, 10.23693724, 13.06401351, 10.89219931, 8.879600778, 12.78620559, 7.5850432, 11.9161311, 8.45829391, 9.914895922, 9.035025663, 11.14112757, 12.38130714, 10.64390577, 9.971073929, 12.66927066, 6.138724148, 11.26766693, 7.643535324, 11.54297146, 12.08648236, 12.48768565, 10.78893745, 9.16152223, 11.15329075, 11.1118742, 15.98419077, 12.25600676, 7.71190731, 7.917913991, 13.37245674, 11.25372271, 10.55719339, 9.405045999, 9.751553245, 10.80679332, 11.70989079, 13.01469023, 9.106822102, 9.216994523, 7.559367626, 9.376298071, 9.90530559, 13.78376241, 8.097489326, 7.087495406, 8.912938417, 9.56797907, 14.28588306, 9.774186717, 9.760616634, 8.580391979, 9.518141308, 6.69864333, 11.68438717, 8.623118338, 12.24798706, 10.00011651, 11.80415363, 11.53418192, 10.94645052, 7.34530534, 7.780129252, 11.34119454, 7.616920652, 9.549031975, 11.72734567, 9.108185977, 10.72663011, 12.56117633, 11.21185276, 8.665715948, 11.11453172, 11.02641744, 5.090845486, 7.511098881, 7.720250917, 9.629370417, 10.56071062, 12.28016685, 9.277918232, 10.98848298, 12.04687763, 12.05506869, 10.48700206, 10.5801717, 10.07166479, 9.067773823, 7.986321596, 10.06681313, 8.259755088, 10.97392444, 7.778686051, 8.38064473, 9.418168233, 12.2571403, 6.420825465, 8.604638583, 6.956058706, 9.595843018, 10.81820897, 11.98918437, 12.56791589, 5.450026494, 8.62473522, 9.466653681, 12.77722454, 8.97371674, 6.064379053, 7.833605723, 9.946832793, 13.59488305, 10.65712013, 12.56173557, 9.787664921, 10.06012463, 11.62209929, 12.19124471, 8.342441541, 11.08905132, 8.034732537, 11.29719205, 10.62841181, 10.48816405, 8.249872408, 6.930498426, 11.35634697, 11.78214969, 13.39172382, 8.428369112, 8.055122593, 7.755772629, 7.847808956, 7.702686177, 9.272985163, 10.99042467, 12.06441041, 9.912016297, 12.11107716, 13.09980124, 15.39462358, 10.64372283, 11.47178194, 9.578530307, 7.72146511, 9.779252378, 10.66951638, 8.831678192, 13.09804218, 10.08843278, 14.45622048, 11.25546519, 9.285523542, 8.792101997, 9.266622617, 11.95563068, 13.3187759, 13.93923562, 6.630056352, 8.113661931, 11.91485969, 9.023155527, 9.884432038, 12.57821719, 9.940001369, 10.40350586, 7.249646888, 10.92220117, 7.482451323, 11.52536931, 10.17529205, 8.440652654, 11.25880217, 11.18202236, 9.193198925, 9.007422072, 5.271243625, 11.47833047, 11.46488739, 7.77128348, 9.317440138, 6.534973203, 5.862468486, 11.72480139, 8.435105236, 9.003301626, 7.855649282, 11.5011026, 10.72068431, 11.56942398, 11.26136562, 9.271264572, 10.4298181, 10.33925825, 9.093042365, 11.45510444, 10.3832901, 9.91369586, 9.734941201, 14.35706006, 6.574268073, 15.65705566, 9.848575217, 11.03192035, 12.27510209, 11.98745154, 11.17331648, 12.72106479, 12.97508868, 6.949830756, 8.545038949, 9.839653215, 9.550370681, 9.466935592, 8.292219596, 10.98282379, 10.70305289, 7.694825659, 11.51381307, 6.756046541, 10.64474217, 10.10713876, 10.31562228, 11.61915145, 8.132016196, 10.83575505, 13.42518992, 10.67348062, 10.71031544, 8.401554973, 7.556459254, 9.161462112, 8.842153365, 10.49331511, 11.94176883, 12.64019268, 6.221789924, 12.53171131, 10.57400901, 9.144431936, 8.123999491, 9.072207297, 7.358622238, 15.8351941, 11.14381429, 10.24372758, 8.22965001, 9.218904723, 8.670324868, 9.361749634, 10.22015986, 12.80069803, 13.28203347, 7.048810705, 9.313636228, 6.352490729, 8.652869461, 8.113006228, 3.644845828, 10.34143262, 12.12895455, 6.390100375, 7.194733296, 9.414687706, 9.505964767, 10.78414977, 10.20982644, 7.861671274, 10.35152518, 8.35784499, 6.328289223, 12.69464829, 10.74293204, 5.576377517, 5.61155721, 9.311442375, 8.838725141, 13.91954473, 8.336780004, 12.30163235, 7.144665249, 11.53759054, 9.389436715, 9.606377125, 13.68654054, 8.314448533, 9.221622893, 12.25009157, 8.520688275, 10.21466709, 9.853759295, 9.045205688, 10.38172814, 7.138918087, 10.52096563, 8.663849066, 8.031470525, 10.77398875, 13.09795455, 13.96153813, 6.087038852, 8.908661214, 11.89816136, 10.89562234, 7.637713531, 8.504049268, 9.009030167, 11.39205941, 10.80399803, 10.42032663, 10.02564525, 10.61736152, 9.239801808, 8.163471402, 8.521657081, 10.89779137, 7.065767906, 8.440233818, 7.627834506, 11.40728688, 9.360870522, 10.87828014)
}
