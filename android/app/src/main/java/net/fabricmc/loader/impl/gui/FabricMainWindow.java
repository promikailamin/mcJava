package net.fabricmc.loader.impl.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import net.fabricmc.loader.impl.util.StringUtil;

class FabricMainWindow {
   static Icon missingIcon = null;

   static void open(FabricStatusTree tree, boolean shouldWait) throws Exception {
      if (GraphicsEnvironment.isHeadless()) {
         throw new HeadlessException();
      }

      System.setProperty("apple.awt.application.appearance", "system");
      System.setProperty("apple.awt.application.name", tree.title);
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      open0(tree, shouldWait);
   }

   private static void open0(FabricStatusTree tree, boolean shouldWait) throws Exception {
      CountDownLatch guiTerminatedLatch = new CountDownLatch(1);
      SwingUtilities.invokeAndWait(() -> createUi(guiTerminatedLatch, tree));
      if (shouldWait) {
         guiTerminatedLatch.await();
      }
   }

   private static void createUi(final CountDownLatch onCloseLatch, FabricStatusTree tree) {
      JFrame window = new JFrame();
      window.setVisible(false);
      window.setTitle(tree.title);

      try {
         Image image = loadImage("/ui/icon/fabric_x128.png");
         window.setIconImage(image);
         setTaskBarImage(image);
      } catch (IOException e) {
         e.printStackTrace();
      }

      window.setMinimumSize(new Dimension(640, 480));
      window.setPreferredSize(new Dimension(800, 480));
      window.setLocationByPlatform(true);
      window.setDefaultCloseOperation(2);
      window.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosed(WindowEvent e) {
            onCloseLatch.countDown();
         }
      });
      Container contentPane = window.getContentPane();
      if (tree.mainText != null && !tree.mainText.isEmpty()) {
         JLabel errorLabel = new JLabel(tree.mainText);
         errorLabel.setHorizontalAlignment(0);
         Font font = errorLabel.getFont();
         errorLabel.setFont(font.deriveFont(font.getSize() * 2.0F));
         contentPane.add(errorLabel, "North");
      }

      FabricMainWindow.IconSet icons = new FabricMainWindow.IconSet();
      if (tree.tabs.isEmpty()) {
         FabricStatusTree.FabricStatusTab tab = new FabricStatusTree.FabricStatusTab("Opening Errors");
         tab.addChild("No tabs provided! (Something is very broken)").setError();
         contentPane.add(createTreePanel(tab.node, tab.filterLevel, icons), "Center");
      } else if (tree.tabs.size() == 1) {
         FabricStatusTree.FabricStatusTab tab = tree.tabs.get(0);
         contentPane.add(createTreePanel(tab.node, tab.filterLevel, icons), "Center");
      } else {
         JTabbedPane tabs = new JTabbedPane();
         contentPane.add(tabs, "Center");

         for (FabricStatusTree.FabricStatusTab tab : tree.tabs) {
            tabs.addTab(tab.node.name, createTreePanel(tab.node, tab.filterLevel, icons));
         }
      }

      if (!tree.buttons.isEmpty()) {
         JPanel buttons = new JPanel();
         contentPane.add(buttons, "South");
         buttons.setLayout(new FlowLayout(4));

         for (FabricStatusTree.FabricStatusButton button : tree.buttons) {
            JButton btn = new JButton(button.text);
            buttons.add(btn);
            btn.addActionListener(event -> {
               if (button.type == FabricStatusTree.FabricBasicButtonType.CLICK_ONCE) {
                  btn.setEnabled(false);
               }

               if (button.clipboard != null) {
                  try {
                     StringSelection clipboard = new StringSelection(button.clipboard);
                     Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboard, clipboard);
                  } catch (IllegalStateException var6) {
                  }
               }

               if (button.shouldClose) {
                  window.dispose();
               }

               if (button.shouldContinue) {
                  onCloseLatch.countDown();
               }
            });
         }
      }

      window.pack();
      window.setVisible(true);
      window.requestFocus();
   }

   private static JPanel createTreePanel(
      FabricStatusTree.FabricStatusNode rootNode, FabricStatusTree.FabricTreeWarningLevel minimumWarningLevel, FabricMainWindow.IconSet iconSet
   ) {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, 1));
      TreeNode treeNode = new FabricMainWindow.CustomTreeNode(null, rootNode, minimumWarningLevel);
      DefaultTreeModel model = new DefaultTreeModel(treeNode);
      JTree tree = new JTree(model);
      tree.setRootVisible(false);
      tree.setRowHeight(0);

      for (int row = 0; row < tree.getRowCount(); row++) {
         if (tree.isVisible(tree.getPathForRow(row))) {
            FabricMainWindow.CustomTreeNode node = (FabricMainWindow.CustomTreeNode)tree.getPathForRow(row).getLastPathComponent();
            if (node.node.expandByDefault) {
               tree.expandRow(row);
            }
         }
      }

      ToolTipManager.sharedInstance().registerComponent(tree);
      tree.setCellRenderer(new FabricMainWindow.CustomTreeCellRenderer(iconSet));
      JScrollPane scrollPane = new JScrollPane(tree);
      panel.add(scrollPane);
      return panel;
   }

   private static BufferedImage loadImage(String str) throws IOException {
      return ImageIO.read(loadStream(str));
   }

   private static InputStream loadStream(String str) throws FileNotFoundException {
      InputStream stream = FabricMainWindow.class.getResourceAsStream(str);
      if (stream == null) {
         throw new FileNotFoundException(str);
      } else {
         return stream;
      }
   }

   private static void setTaskBarImage(Image image) {
      try {
         Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
         Method getTaskbar = taskbarClass.getDeclaredMethod("getTaskbar");
         Method setIconImage = taskbarClass.getDeclaredMethod("setIconImage", Image.class);
         Object taskbar = getTaskbar.invoke(null);
         setIconImage.invoke(taskbar, image);
      } catch (Exception var5) {
      }
   }

   private static Icon missingIcon() {
      if (missingIcon == null) {
         BufferedImage img = new BufferedImage(16, 16, 1);

         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               img.setRGB(x, y, 16777202);
            }
         }

         for (int i = 0; i < 16; i++) {
            img.setRGB(0, i, 2236962);
            img.setRGB(15, i, 2236962);
            img.setRGB(i, 0, 2236962);
            img.setRGB(i, 15, 2236962);
         }

         for (int i = 3; i < 13; i++) {
            img.setRGB(i, i, 10158080);
            img.setRGB(i, 16 - i, 10158080);
         }

         missingIcon = new ImageIcon(img);
      }

      return missingIcon;
   }

   private static Icon loadIcon(FabricMainWindow.IconInfo info, int scale) throws IOException {
      BufferedImage img = new BufferedImage(scale, scale, 2);
      Graphics2D imgG2d = img.createGraphics();
      BufferedImage main = loadImage("/ui/icon/" + info.mainPath + "_x" + scale + ".png");
      assert main.getWidth() == scale;
      assert main.getHeight() == scale;
      imgG2d.drawImage(main, null, 0, 0);
      int[][] coords = new int[][]{{0, 8}, {8, 8}, {8, 0}};

      for (int i = 0; i < info.decor.length; i++) {
         String decor = info.decor[i];
         if (decor != null) {
            BufferedImage decorImg = loadImage("/ui/icon/decoration/" + decor + "_x" + scale / 2 + ".png");
            assert decorImg.getWidth() == scale / 2;
            assert decorImg.getHeight() == scale / 2;
            imgG2d.drawImage(decorImg, null, coords[i][0], coords[i][1]);
         }
      }

      return new ImageIcon(img);
   }

   private static String applyWrapping(String str) {
      if (str.indexOf(10) < 0) {
         return str;
      }

      str = str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
      return "<html>" + str + "</html>";
   }

   private static final class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
      private static final long serialVersionUID = -5621219150752332739L;
      private final FabricMainWindow.IconSet iconSet;

      private CustomTreeCellRenderer(FabricMainWindow.IconSet icons) {
         this.iconSet = icons;
      }

      @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
         super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
         this.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
         if (value instanceof FabricMainWindow.CustomTreeNode) {
            FabricMainWindow.CustomTreeNode c = (FabricMainWindow.CustomTreeNode)value;
            this.setIcon(this.iconSet.get(c.getIconInfo()));
            if (c.node.details != null && !c.node.details.isEmpty()) {
               this.setToolTipText(FabricMainWindow.applyWrapping(c.node.details));
            } else {
               this.setToolTipText(null);
            }
         }

         return this;
      }
   }

   static class CustomTreeNode implements TreeNode {
      public final TreeNode parent;
      public final FabricStatusTree.FabricStatusNode node;
      public final List<FabricMainWindow.CustomTreeNode> displayedChildren = new ArrayList<>();
      private FabricMainWindow.IconInfo iconInfo;

      CustomTreeNode(TreeNode parent, FabricStatusTree.FabricStatusNode node, FabricStatusTree.FabricTreeWarningLevel minimumWarningLevel) {
         this.parent = parent;
         this.node = node;

         for (FabricStatusTree.FabricStatusNode c : node.children) {
            if (!minimumWarningLevel.isHigherThan(c.getMaximumWarningLevel())) {
               this.displayedChildren.add(new FabricMainWindow.CustomTreeNode(this, c, minimumWarningLevel));
            }
         }
      }

      public FabricMainWindow.IconInfo getIconInfo() {
         if (this.iconInfo == null) {
            this.iconInfo = FabricMainWindow.IconInfo.fromNode(this.node);
         }

         return this.iconInfo;
      }

      @Override
      public String toString() {
         return FabricMainWindow.applyWrapping(StringUtil.wrapLines(this.node.name, 120));
      }

      @Override
      public TreeNode getChildAt(int childIndex) {
         return this.displayedChildren.get(childIndex);
      }

      @Override
      public int getChildCount() {
         return this.displayedChildren.size();
      }

      @Override
      public TreeNode getParent() {
         return this.parent;
      }

      @Override
      public int getIndex(TreeNode node) {
         return this.displayedChildren.indexOf(node);
      }

      @Override
      public boolean getAllowsChildren() {
         return !this.isLeaf();
      }

      @Override
      public boolean isLeaf() {
         return this.displayedChildren.isEmpty();
      }

      @Override
      public Enumeration<FabricMainWindow.CustomTreeNode> children() {
         return new Enumeration<FabricMainWindow.CustomTreeNode>() {
            Iterator<FabricMainWindow.CustomTreeNode> it = CustomTreeNode.this.displayedChildren.iterator();

            @Override
            public boolean hasMoreElements() {
               return this.it.hasNext();
            }

            public FabricMainWindow.CustomTreeNode nextElement() {
               return this.it.next();
            }
         };
      }
   }

   static final class IconInfo {
      public final String mainPath;
      public final String[] decor;
      private final int hash;

      IconInfo(String mainPath) {
         this.mainPath = mainPath;
         this.decor = new String[0];
         this.hash = mainPath.hashCode();
      }

      IconInfo(String mainPath, String[] decor) {
         this.mainPath = mainPath;
         this.decor = decor;
         assert decor.length < 4 : "Cannot fit more than 3 decorations into an image (and leave space for the background)";
         if (decor.length == 0) {
            this.hash = mainPath.hashCode();
         } else {
            this.hash = mainPath.hashCode() * 31 + Arrays.hashCode(decor);
         }
      }

      public static FabricMainWindow.IconInfo fromNode(FabricStatusTree.FabricStatusNode node) {
         String[] split = node.iconType.split("\\+");
         if (split.length == 1 && split[0].isEmpty()) {
            split = new String[0];
         }

         List<String> decors = new ArrayList<>();
         FabricStatusTree.FabricTreeWarningLevel warnLevel = node.getMaximumWarningLevel();
         String main;
         if (split.length == 0) {
            if (warnLevel == FabricStatusTree.FabricTreeWarningLevel.NONE) {
               main = "missing";
            } else {
               main = "level_" + warnLevel.lowerCaseName;
            }
         } else {
            main = split[0];
            if (warnLevel == FabricStatusTree.FabricTreeWarningLevel.NONE) {
               decors.add(null);
            } else {
               decors.add("level_" + warnLevel.lowerCaseName);
            }

            for (int i = 1; i < split.length && i < 3; i++) {
               decors.add(split[i]);
            }
         }

         return new FabricMainWindow.IconInfo(main, decors.toArray(new String[0]));
      }

      @Override
      public int hashCode() {
         return this.hash;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else if (obj != null && obj.getClass() == this.getClass()) {
            FabricMainWindow.IconInfo other = (FabricMainWindow.IconInfo)obj;
            return this.mainPath.equals(other.mainPath) && Arrays.equals(this.decor, other.decor);
         } else {
            return false;
         }
      }
   }

   static final class IconSet {
      private final Map<FabricMainWindow.IconInfo, Map<Integer, Icon>> icons = new HashMap<>();

      public Icon get(FabricMainWindow.IconInfo info) {
         int scale = 16;
         Map<Integer, Icon> map = this.icons.get(info);
         if (map == null) {
            this.icons.put(info, map = new HashMap<>());
         }

         Icon icon = map.get(scale);
         if (icon == null) {
            try {
               icon = FabricMainWindow.loadIcon(info, scale);
            } catch (IOException e) {
               e.printStackTrace();
               icon = FabricMainWindow.missingIcon();
            }

            map.put(scale, icon);
         }

         return icon;
      }
   }
}
