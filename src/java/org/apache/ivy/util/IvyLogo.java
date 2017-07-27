/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import static java.awt.Color.WHITE;
import static java.lang.Math.min;

/**
 * This class has been automatically generated using
 * <a href="http://ebourg.github.io/flamingo-svg-transcoder/">Flamingo SVG transcoder</a>.
 */
public class IvyLogo implements Icon {

    /** Maximum dimension of an icon in JInfoPane */
    private static final int MAX_INFO_ICON_SIZE = 128;

    /** The colour of this icon. */
    private static final Color COLOUR = new Color(0x6E9244);

    /** The width of this icon. */
    private int width;

    /** The height of this icon. */
    private int height;

    /** The rendered image. */
    private BufferedImage image;

    /**
     * Creates a new Ivy logo transcoded from SVG image.
     */
    public IvyLogo() {
        this(MAX_INFO_ICON_SIZE, MAX_INFO_ICON_SIZE);
    }

    /**
     * Creates a new Ivy logo transcoded from SVG image.
     *
     * @param width image dimension
     * @param height image dimension
     */
    public IvyLogo(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (image == null) {
            image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            double coef = min((double) width, (double) height);

            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.scale(coef, coef);
            paint(g2d);
            g2d.dispose();
        }

        g.drawImage(image, x, y, null);
    }

    /**
     * Paints the Ivy logo transcoded from SVG image on the specified graphics context.
     *
     * @param g Graphics context.
     */
    private static void paint(Graphics2D g) {
        LinkedList<AffineTransform> transformations = new LinkedList<>();

        transformations.offer(g.getTransform());
        g.transform(new AffineTransform(0.002013243f, 0, 0, 0.002013243f, 0, 0.1471439f));

        GeneralPath shape = new GeneralPath();
        shape.moveTo(35.696, 151.511);
        shape.lineTo(62.092, 151.511);
        shape.curveTo(57.965, 148.382, 52.366997, 144.04001, 52.321, 140.41);
        shape.curveTo(52.299, 139.578, 52.273, 138.417, 52.203, 137.017);
        shape.curveTo(49.002, 138.843, 36.882, 141.357, 29.575998, 134.83499);
        shape.curveTo(22.129997, 128.16899, 20.846998, 119.81799, 23.196999, 115.88399);
        shape.curveTo(25.544998, 111.969986, 32.066998, 111.875984, 32.066998, 111.875984);
        shape.lineTo(32.377, 108.74499);
        shape.curveTo(32.377, 108.74499, 22.484999, 103.57199, 16.532999, 91.16899);
        shape.curveTo(10.58, 78.766, 6.453, 63.87, 6.453, 63.87);
        shape.curveTo(6.453, 63.87, 13.353001, 59.507, 23.126999, 61.048);
        shape.curveTo(32.898, 62.568, 37.476997, 66.552, 39.822998, 65.745);
        shape.curveTo(42.955, 54.288, 70.421, 3.413, 75.046, -0.003);
        shape.curveTo(89.633, 6.281, 89.941, 43.307, 90.107, 46.13);
        shape.curveTo(90.273, 48.953003, 90.107, 69.825, 90.107, 72.648);
        shape.curveTo(90.107, 75.471, 91.98, 76.798004, 95.919, 77.203);
        shape.curveTo(99.833, 77.605, 126.703995, 60.888, 133.465, 58.681004);
        shape.curveTo(133.465, 82.448006, 118.66499, 103.937004, 111.146996, 112.189);
        shape.curveTo(106.521996, 120.587006, 109.98499, 122.200005, 111.050995, 126.136);
        shape.curveTo(112.117, 130.074, 112.16499, 138.683, 111.548996, 140.321);
        shape.curveTo(107.422, 141.483, 90.297, 141.96, 84.01199, 139.8);
        shape.curveTo(77.728, 137.642, 77.465996, 134.179, 75.57099, 132.779);
        shape.curveTo(75.40499, 132.66301, 64.06699, 131.238, 56.715992, 134.10701);
        shape.lineTo(56.57299, 140.488);
        shape.curveTo(60.79499, 144.38, 63.94999, 145.94301, 67.24599, 151.49501);
        shape.lineTo(154.74199, 151.49501);
        shape.lineTo(185.887, 241.76501);
        shape.lineTo(218.808, 151.49503);
        shape.lineTo(317.356, 151.49503);
        shape.lineTo(352.249, 244.82703);
        shape.lineTo(383.227, 151.49503);
        shape.lineTo(433.93597, 151.49503);
        shape.curveTo(434.22098, 139.23003, 443.80298, 140.75102, 446.649, 140.68103);
        shape.curveTo(446.033, 139.73303, 443.352, 137.78603, 444.253, 134.86903);
        shape.curveTo(445.155, 131.97304, 452.06, 132.87903, 452.06, 132.87903);
        shape.curveTo(452.06, 132.87903, 454.312, 127.51803, 455.378, 125.00403);
        shape.curveTo(456.443, 122.48803, 460.524, 121.897026, 460.524, 121.897026);
        shape.curveTo(460.524, 121.897026, 458.05698, 131.79103, 462.421, 131.79103);
        shape.lineTo(483.07898, 131.79103);
        shape.curveTo(483.07898, 131.79103, 475.51398, 139.07004, 472.309, 139.64403);
        shape.curveTo(469.106, 140.23602, 462.98798, 143.43703, 464.88498, 144.60103);
        shape.curveTo(466.78098, 145.76303, 471.87997, 149.20303, 470.99997, 151.50203);
        shape.curveTo(470.12497, 153.80403, 457.74396, 149.19904, 457.74396, 151.50203);
        shape.curveTo(457.74396, 153.80403, 450.22296, 157.35902, 450.60196, 154.06403);
        shape.curveTo(450.98297, 150.76703, 451.45596, 144.41202, 449.95895, 143.17703);
        shape.curveTo(448.44095, 141.94504, 444.24194, 142.27704, 440.85394, 142.70103);
        shape.curveTo(437.45895, 143.12903, 437.65094, 151.50203, 437.65094, 151.50203);
        shape.lineTo(444.24594, 151.50203);
        shape.lineTo(442.15994, 156.48103);
        shape.curveTo(459.73395, 157.16904, 462.31995, 174.76804, 463.05594, 173.08504);
        shape.curveTo(463.88495, 171.16304, 466.80093, 164.92604, 467.39594, 164.21404);
        shape.curveTo(469.60092, 161.53203, 475.33994, 161.86603, 476.71494, 162.88704);
        shape.curveTo(479.06595, 164.61604, 477.11795, 168.31905, 481.76892, 171.04404);
        shape.curveTo(489.52493, 174.38904, 495.21494, 173.39503, 496.70993, 175.66904);
        shape.curveTo(495.37994, 179.18204, 489.31192, 187.00703, 485.72992, 188.52304);
        shape.curveTo(483.09592, 189.63805, 482.83392, 193.81204, 483.30893, 194.78705);
        shape.curveTo(488.33792, 205.36505, 487.03693, 212.55106, 485.58594, 222.20505);
        shape.curveTo(473.32294, 212.78905, 464.71295, 202.09204, 460.77594, 203.51405);
        shape.curveTo(456.83795, 204.93906, 442.20395, 206.03105, 442.20395, 206.03105);
        shape.curveTo(442.20395, 206.03105, 446.02194, 219.29105, 444.29196, 225.69305);
        shape.curveTo(443.19995, 229.72406, 448.13495, 228.82605, 448.13495, 228.82605);
        shape.curveTo(448.13495, 228.82605, 452.42795, 223.75105, 455.20193, 222.27705);
        shape.curveTo(457.97592, 220.83205, 461.32092, 219.88106, 465.80493, 220.99805);
        shape.curveTo(470.36093, 222.56204, 471.14392, 230.06004, 475.48392, 232.57005);
        shape.curveTo(478.6849, 234.44505, 492.75192, 249.69505, 492.75192, 249.69505);
        shape.curveTo(492.75192, 249.69505, 485.94293, 253.09105, 480.18494, 253.34906);
        shape.curveTo(474.41794, 253.60905, 466.14194, 253.29906, 463.53693, 254.03706);
        shape.curveTo(459.80994, 255.08206, 455.87494, 283.26108, 449.47092, 289.30807);
        shape.curveTo(441.6459, 280.05606, 440.24393, 267.5081, 437.4689, 255.93507);
        shape.curveTo(436.5919, 252.30608, 433.88892, 251.38107, 432.5839, 251.38107);
        shape.curveTo(431.27887, 251.38107, 427.9589, 253.39807, 421.90988, 255.55608);
        shape.curveTo(415.8599, 257.74008, 407.25488, 254.65608, 407.25488, 254.65608);
        shape.lineTo(423.7349, 233.40608);
        shape.curveTo(423.7349, 233.40608, 422.17288, 224.32208, 423.3079, 221.33308);
        shape.curveTo(428.7429, 216.14009, 436.1629, 220.10109, 436.1629, 220.10109);
        shape.lineTo(442.8269, 226.26709);
        shape.curveTo(443.0619, 218.82208, 440.4069, 210.02109, 435.1179, 205.37209);
        shape.curveTo(431.6319, 202.31009, 426.55588, 201.31209, 423.4969, 201.00708);
        shape.lineTo(377.29388, 310.82007);
        shape.curveTo(356.13788, 361.10107, 301.13388, 351.40405, 266.33887, 346.13608);
        shape.lineTo(262.18988, 310.91406);
        shape.curveTo(275.40088, 312.59906, 313.8469, 319.78506, 320.67688, 293.72006);
        shape.lineTo(267.14688, 183.90607);
        shape.lineTo(210.86488, 293.72006);
        shape.lineTo(159.08987, 293.72006);
        shape.lineTo(97.61087, 171.64006);
        shape.lineTo(97.61087, 207.41006);
        shape.curveTo(98.58387, 207.47806, 99.33987, 207.83707, 99.98387, 208.40607);
        shape.curveTo(101.54587, 209.83107, 108.616875, 214.90607, 110.01587, 216.94707);
        shape.curveTo(113.76187, 222.35107, 119.16887, 221.66707, 118.33887, 226.46007);
        shape.curveTo(116.22887, 229.51907, 113.00287, 227.55006, 111.60387, 226.93307);
        shape.curveTo(110.18087, 226.31607, 105.48486, 217.92108, 103.91887, 216.66208);
        shape.curveTo(102.63787, 215.64108, 99.88587, 212.58208, 97.633865, 212.34708);
        shape.lineTo(97.633865, 231.10907);
        shape.curveTo(107.16887, 240.73807, 120.07187, 253.42706, 121.635864, 254.68507);
        shape.curveTo(124.15086, 256.72406, 127.58887, 258.76306, 130.10387, 259.38007);
        shape.curveTo(132.61887, 259.99606, 135.91586, 260.16006, 136.83786, 261.10907);
        shape.curveTo(140.13486, 264.40607, 138.97285, 267.08807, 135.41486, 267.39407);
        shape.curveTo(133.54086, 267.56207, 123.01086, 265.59106, 121.303856, 263.38608);
        shape.curveTo(119.57486, 261.1811, 108.59086, 248.86708, 108.59086, 248.86708);
        shape.curveTo(108.59086, 248.86708, 101.52386, 240.87508, 99.03086, 239.45108);
        shape.curveTo(98.579865, 239.19109, 98.08186, 238.76308, 97.60686, 238.22208);
        shape.lineTo(97.60686, 293.70108);
        shape.lineTo(49.293, 293.70108);
        shape.curveTo(50.076, 295.31207, 51.878998, 299.3921, 50.385, 299.5351);
        shape.curveTo(48.51, 299.70108, 47.398, 299.4391, 46.305, 297.2601);
        shape.curveTo(45.973, 296.5961, 45.760002, 295.2401, 45.641, 293.7261);
        shape.lineTo(35.796997, 293.7261);
        shape.lineTo(35.796997, 236.2561);
        shape.lineTo(35.774998, 236.2561);
        shape.curveTo(35.373997, 236.37311, 34.992996, 236.4901, 34.635, 236.6101);
        shape.curveTo(31.696, 236.70811, 28.822998, 236.30011, 27.258, 236.4441);
        shape.curveTo(23.772999, 236.79811, 11.865, 249.10811, 9.92, 252.5961);
        shape.curveTo(9.137, 254.0201, 6.149, 256.8391, 4.584, 257.2901);
        shape.curveTo(3.0210001, 257.7661, -0.91799974, 254.7741, 0.1960001, 252.7351);
        shape.curveTo(1.2870001, 250.6941, 6.149, 248.0401, 7.261, 247.70811);
        shape.curveTo(8.354, 247.39711, 9.778, 243.31711, 11.507, 241.75612);
        shape.curveTo(13.239, 240.19212, 22.654, 233.90312, 24.220001, 232.02812);
        shape.curveTo(25.785002, 230.15512, 34.419003, 230.29912, 34.419003, 230.29912);
        shape.lineTo(35.843002, 230.44212);
        shape.lineTo(35.843002, 204.68213);
        shape.curveTo(35.276, 204.04112, 34.869003, 203.59213, 34.751003, 203.40112);
        shape.curveTo(33.495003, 201.43213, 31.999002, 204.58913, 32.476, 208.73912);
        shape.curveTo(32.953003, 212.88712, 31.384003, 220.45612, 31.384003, 222.16312);
        shape.curveTo(31.384003, 224.37012, 37.124, 227.57112, 33.735004, 229.58812);
        shape.curveTo(33.047005, 229.99112, 30.341003, 229.37512, 28.162004, 225.29512);
        shape.curveTo(25.955004, 221.21712, 28.162004, 205.98912, 28.162004, 205.98912);
        shape.curveTo(28.162004, 205.98912, 28.588005, 197.26012, 32.218006, 196.95212);
        shape.curveTo(33.904007, 196.80711, 35.088005, 196.80711, 35.869007, 196.88112);
        shape.lineTo(35.869007, 151.43811);
        shape.lineTo(35.696, 151.511);
        shape.closePath();
        shape.moveTo(440.661, 160.026);
        shape.lineTo(426.073, 194.653);
        shape.curveTo(434.757, 197.786, 441.589, 204.946, 441.635, 204.924);
        shape.curveTo(443.43802, 204.094, 444.21902, 203.621, 444.21902, 201.43799);
        shape.curveTo(444.21902, 199.23299, 445.31104, 195.53398, 445.97504, 191.97499);
        shape.curveTo(447.06903, 186.18799, 443.50803, 187.20499, 442.82303, 182.512);
        shape.curveTo(441.82703, 175.633, 451.97504, 172.502, 455.88904, 175.01599);
        shape.curveTo(456.91003, 175.657, 456.45804, 177.221, 457.57504, 177.86198);
        shape.curveTo(458.66705, 178.47899, 460.44403, 178.19398, 460.80203, 177.62599);
        shape.curveTo(462.62802, 174.54199, 460.89703, 174.258, 458.90604, 170.581);
        shape.curveTo(455.34503, 159.34, 444.01004, 160.50099, 440.63803, 160.026);
        shape.lineTo(440.661, 160.026);
        shape.lineTo(440.661, 160.026);
        shape.closePath();

        g.setPaint(COLOUR);
        g.fill(shape);

        shape = new GeneralPath();
        shape.moveTo(35.72, 196.954);
        shape.curveTo(36.574, 197.024, 36.93, 197.12, 36.93, 197.12);
        shape.curveTo(36.93, 197.12, 38.187, 197.288, 41.010002, 201.034);
        shape.curveTo(43.833, 204.805, 45.090004, 208.176, 47.605003, 210.928);
        shape.curveTo(50.121002, 213.68, 52.541004, 211.94899, 52.541004, 211.94899);
        shape.curveTo(52.541004, 211.94899, 50.430004, 200.34999, 51.995003, 195.93799);
        shape.curveTo(53.558002, 191.551, 55.099003, 190.60199, 56.667004, 189.344);
        shape.curveTo(58.232002, 188.086, 60.557003, 186.07199, 58.354004, 184.032);
        shape.curveTo(56.147003, 181.991, 54.487003, 172.644, 57.477005, 166.053);
        shape.curveTo(60.463005, 159.459, 66.490005, 157.633, 66.490005, 157.633);
        shape.curveTo(66.490005, 157.633, 68.62401, 156.897, 65.28001, 154.004);
        shape.curveTo(64.49701, 153.316, 63.335007, 152.463, 62.051006, 151.465);
        shape.lineTo(67.19701, 151.465);
        shape.lineTo(69.90101, 154.668);
        shape.curveTo(78.65301, 150.444, 87.78401, 154.286, 87.78401, 162.922);
        shape.curveTo(89.041016, 163.868, 95.42201, 165.93399, 98.388016, 165.93399);
        shape.lineTo(113.75501, 165.93399);
        shape.curveTo(115.321014, 165.93399, 129.126, 157.963, 131.16501, 157.82098);
        shape.curveTo(133.20401, 157.65498, 133.27501, 154.33798, 140.81702, 155.35599);
        shape.curveTo(148.35902, 156.374, 148.26402, 155.094, 149.35602, 157.18199);
        shape.curveTo(150.44702, 159.29, 147.55302, 161.405, 145.82202, 162.04199);
        shape.curveTo(144.09302, 162.659, 134.43703, 161.68799, 132.87202, 161.68799);
        shape.curveTo(131.31003, 161.68799, 119.68402, 167.452, 116.011024, 169.25299);
        shape.curveTo(112.33302, 171.05699, 104.484024, 171.69398, 101.65803, 170.2);
        shape.curveTo(98.83703, 168.706, 93.16703, 167.594, 93.16703, 167.594);
        shape.lineTo(88.25903, 166.194);
        shape.curveTo(88.25903, 166.194, 88.02303, 176.798, 86.15003, 181.213);
        shape.curveTo(84.25303, 185.59999, 76.02303, 191.481, 73.53103, 191.481);
        shape.curveTo(71.01703, 191.481, 72.17903, 193.475, 72.17903, 193.475);
        shape.curveTo(74.716034, 198.502, 73.270035, 205.26201, 67.10303, 210.362);
        shape.curveTo(69.68803, 217.524, 81.26303, 197.602, 84.205025, 197.602);
        shape.curveTo(86.86102, 197.602, 87.40602, 198.811, 86.97903, 201.63301);
        shape.lineTo(77.065025, 212.49402);
        shape.curveTo(77.065025, 212.49402, 73.649025, 214.76802, 70.94602, 214.60402);
        shape.curveTo(70.163025, 214.55702, 69.45202, 215.76602, 69.689026, 217.49802);
        shape.curveTo(69.998024, 219.70302, 69.287025, 221.51001, 70.54302, 222.60002);
        shape.curveTo(71.803024, 223.69202, 76.71102, 220.98701, 78.301025, 219.11401);
        shape.curveTo(79.86403, 217.24101, 87.40602, 211.07301, 91.678024, 208.98701);
        shape.curveTo(94.19302, 207.77802, 96.01802, 207.30301, 97.41602, 207.39902);
        shape.lineTo(97.440025, 212.33401);
        shape.curveTo(96.91902, 212.28502, 96.39603, 212.38301, 95.94602, 212.71501);
        shape.curveTo(93.59702, 214.445, 87.09902, 219.23601, 87.09902, 220.16002);
        shape.curveTo(87.09902, 220.51402, 90.06202, 222.51102, 97.413025, 231.07002);
        shape.lineTo(97.413025, 238.21303);
        shape.curveTo(95.27802, 235.84203, 93.024025, 231.38103, 91.246025, 230.35902);
        shape.curveTo(89.06303, 229.10002, 86.93102, 225.02303, 86.31103, 223.62302);
        shape.curveTo(85.69603, 222.20102, 83.32403, 223.00801, 82.84803, 224.71701);
        shape.curveTo(82.37103, 226.447, 81.68603, 228.18001, 80.24003, 230.48201);
        shape.curveTo(76.77703, 236.033, 70.25703, 236.529, 69.28203, 237.503);
        shape.curveTo(68.191025, 238.595, 71.86703, 241.179, 72.816025, 242.294);
        shape.curveTo(73.76502, 243.386, 81.189026, 245.356, 83.72603, 254.772);
        shape.curveTo(86.24103, 264.188, 83.74803, 271.327, 82.35103, 275.09702);
        shape.curveTo(79.28903, 283.282, 73.74303, 283.993, 64.064026, 279.15503);
        shape.curveTo(62.949024, 278.58502, 54.932026, 269.68903, 53.225025, 265.75302);
        shape.curveTo(51.495026, 261.84103, 53.391026, 249.05602, 53.391026, 249.05602);
        shape.curveTo(53.391026, 249.05602, 56.377026, 244.98001, 54.953026, 244.81401);
        shape.curveTo(54.361027, 244.742, 54.810024, 242.988, 53.246025, 243.699);
        shape.curveTo(51.086025, 244.695, 46.891026, 248.253, 46.891026, 248.253);
        shape.lineTo(46.655025, 280.726);
        shape.curveTo(46.655025, 280.726, 48.528027, 280.62802, 48.860023, 283.122);
        shape.curveTo(49.16902, 285.59, 48.860023, 293.083, 48.860023, 293.083);
        shape.lineTo(49.192024, 293.747);
        shape.lineTo(45.54, 293.747);
        shape.curveTo(45.255, 290.237, 45.421, 285.706, 45.421, 285.706);
        shape.curveTo(45.421, 285.706, 43.525, 282.50598, 42.409, 281.102);
        shape.curveTo(41.317, 279.68, 41.343002, 250.81499, 42.907, 246.56898);
        shape.curveTo(44.471, 242.32498, 45.563, 240.68799, 47.768, 240.99498);
        shape.curveTo(49.974003, 241.30698, 53.27, 240.82898, 53.27, 240.82898);
        shape.curveTo(53.27, 240.82898, 54.053, 230.08798, 52.793, 229.44598);
        shape.curveTo(51.537, 228.82898, 46.364998, 232.26799, 45.584, 233.52399);
        shape.curveTo(44.873, 234.663, 39.514, 235.18399, 35.668, 236.29898);
        shape.lineTo(35.739998, 230.53198);
        shape.lineTo(39.155, 230.86398);
        shape.curveTo(39.155, 230.86398, 42.830997, 228.34998, 44.729, 227.23499);
        shape.curveTo(46.603, 226.14499, 51.939, 226.33798, 52.414, 224.67299);
        shape.curveTo(52.891003, 223.01099, 52.959, 217.67499, 52.58, 216.846);
        shape.curveTo(52.18, 216.01599, 50.018, 218.15099, 48.5, 216.72699);
        shape.lineTo(35.716, 204.77399);
        shape.lineTo(35.716, 196.96498);
        shape.lineTo(35.739, 196.96498);
        shape.lineTo(35.718, 196.96498);
        shape.lineTo(35.72, 196.954);
        shape.lineTo(35.72, 196.954);
        shape.closePath();
        shape.moveTo(52.18, 136.687);
        shape.curveTo(51.87, 130.023, 51.159, 118.948, 51.041, 118.044);
        shape.curveTo(50.875, 116.952995, 44.448, 109.103, 44.448, 106.921);
        shape.curveTo(44.448, 104.714, 42.739002, 100.492, 46.985, 101.751);
        shape.curveTo(51.231, 103.009, 53.58, 107.253, 53.58, 107.253);
        shape.curveTo(53.58, 107.253, 57.114002, 113.205, 57.114002, 114.772995);
        shape.curveTo(57.114002, 115.815994, 56.878002, 126.657, 56.711002, 133.937);
        shape.lineTo(52.182003, 136.71199);
        shape.lineTo(52.182003, 136.687);
        shape.lineTo(52.18, 136.687);
        shape.closePath();

        g.setPaint(WHITE);
        g.fill(shape);

        g.setTransform(transformations.poll());
    }
}
