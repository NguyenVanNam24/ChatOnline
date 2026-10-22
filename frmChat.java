/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package chatonline.gui;

import chatonline.database.ConnectDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

/**
 *
 * @author ASUS
 */
public class frmChat extends javax.swing.JFrame {

    private int currentUserId;
    private String currentDisplayName;
    private int selectedUserId = -1;
    private javax.swing.Timer timerTinNhan;
    private String noiDungTinNhanCu = "";

    public frmChat(int userId, String displayName) {
        initComponents();

        currentUserId = userId;
        currentDisplayName = displayName;
        lblTaiKhoan.setText("Tài khoản đang đăng nhập: " + currentDisplayName);
        loadDanhSachNguoiDung();

        timerTinNhan = new javax.swing.Timer(1000, e -> {
            if (selectedUserId != -1) {
                loadLichSuTinNhan(selectedUserId);
            }
        });

        timerTinNhan.start();
    }

    private void hienThiTinNhan(String noiDung, boolean laTinNhanCuaBan) {
        javax.swing.text.StyledDocument doc = txtKhungChat.getStyledDocument();
        javax.swing.text.SimpleAttributeSet style = new javax.swing.text.SimpleAttributeSet();

        javax.swing.text.StyleConstants.setAlignment(
                style,
                laTinNhanCuaBan
                        ? javax.swing.text.StyleConstants.ALIGN_RIGHT
                        : javax.swing.text.StyleConstants.ALIGN_LEFT
        );

        javax.swing.text.StyleConstants.setSpaceAbove(style, 5);
        javax.swing.text.StyleConstants.setSpaceBelow(style, 5);

        try {
            int viTri = doc.getLength();

            doc.insertString(viTri, noiDung + "\n", null);

            doc.setParagraphAttributes(
                    viTri,
                    noiDung.length() + 1,
                    style,
                    false
            );
        } catch (javax.swing.text.BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void loadDanhSachNguoiDung() {
        DefaultListModel<String> model = new DefaultListModel<>();

        String sql = "SELECT UserID, DisplayName "
                + "FROM Users WHERE UserID <> ? ORDER BY DisplayName";

        try (Connection conn = ConnectDB.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, currentUserId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int userId = rs.getInt("UserID");
                    String displayName = rs.getString("DisplayName");

                    model.addElement(userId + "|" + displayName);
                }
            }

            listBanBe.setModel(model);

            listBanBe.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(
                        javax.swing.JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {

                    super.getListCellRendererComponent(
                            list, value, index, isSelected, cellHasFocus);

                    String item = value.toString();
                    setText(item.substring(item.indexOf("|") + 1));

                    return this;
                }
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải danh sách người dùng: " + e.getMessage());
        }
    }

    private void timKiemNguoiDung(String tuKhoa) {
        javax.swing.DefaultListModel<String> model
                = new javax.swing.DefaultListModel<>();

        String sql = "SELECT UserID, DisplayName FROM Users "
                + "WHERE UserID <> ? AND DisplayName LIKE ? "
                + "ORDER BY DisplayName";

        try (java.sql.Connection conn
                = chatonline.database.ConnectDB.getConnection(); java.sql.PreparedStatement pst
                = conn.prepareStatement(sql)) {

            pst.setInt(1, currentUserId);
            pst.setString(2, "%" + tuKhoa.trim() + "%");

            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int userId = rs.getInt("UserID");
                    String displayName = rs.getString("DisplayName");

                    model.addElement(userId + "|" + displayName);
                }
            }

            listBanBe.setModel(model);

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Lỗi tìm kiếm người dùng: " + e.getMessage()
            );
        }
    }

    private void loadLichSuTinNhan(int otherUserId) {
        String sql
                = "SELECT m.SenderID, m.MessageContent "
                + "FROM Messages m "
                + "JOIN Conversations c "
                + "ON m.ConversationID = c.ConversationID "
                + "JOIN ConversationMembers cm1 "
                + "ON c.ConversationID = cm1.ConversationID "
                + "JOIN ConversationMembers cm2 "
                + "ON c.ConversationID = cm2.ConversationID "
                + "WHERE cm1.UserID = ? "
                + "AND cm2.UserID = ? "
                + "AND (SELECT COUNT(*) "
                + "FROM ConversationMembers cm3 "
                + "WHERE cm3.ConversationID = c.ConversationID) = 2 "
                + "ORDER BY m.SentAt, m.MessageID";

        StringBuilder noiDungMoi = new StringBuilder();

        try (Connection conn = ConnectDB.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, currentUserId);
            pst.setInt(2, otherUserId);

            try (ResultSet rs = pst.executeQuery()) {
                String tenNguoiNhan = "";

                String giaTriDangChon = listBanBe.getSelectedValue();

                if (giaTriDangChon != null) {
                    String[] parts = giaTriDangChon.split("\\|", 2);
                    if (parts.length == 2) {
                        tenNguoiNhan = parts[1];
                    }
                }

                while (rs.next()) {
                    int senderId = rs.getInt("SenderID");
                    String content = rs.getString("MessageContent");

                    String senderName = senderId == currentUserId
                            ? "Bạn"
                            : tenNguoiNhan;

                    noiDungMoi.append(senderName)
                            .append(": ")
                            .append(content == null ? "" : content)
                            .append("\n");
                }
            }

            String noiDung = noiDungMoi.toString();

            if (!noiDung.equals(noiDungTinNhanCu)) {

                hienThiLaiLichSu(otherUserId, noiDung);
                noiDungTinNhanCu = noiDung;
            }

        } catch (Exception e) {
            System.err.println("Lỗi tải lịch sử tin nhắn: " + e.getMessage());
        }

    }

    private void hienThiLaiLichSu(int otherUserId, String noiDung) {
        txtKhungChat.setText("");

        String[] dongTinNhan = noiDung.split("\n");

        for (String dong : dongTinNhan) {
            if (!dong.isEmpty()) {
                boolean laTinNhanCuaBan = dong.startsWith("Bạn: ");
                hienThiTinNhan(dong, laTinNhanCuaBan);
            }
        }
    }

    private int getOrCreateConversationId(int userId1, int userId2)
            throws java.sql.SQLException {

        String findSql
                = "SELECT c.ConversationID "
                + "FROM Conversations c "
                + "JOIN ConversationMembers cm1 "
                + "ON c.ConversationID = cm1.ConversationID "
                + "JOIN ConversationMembers cm2 "
                + "ON c.ConversationID = cm2.ConversationID "
                + "WHERE cm1.UserID = ? "
                + "AND cm2.UserID = ? "
                + "AND (SELECT COUNT(*) "
                + "FROM ConversationMembers cm3 "
                + "WHERE cm3.ConversationID = c.ConversationID) = 2";

        try (Connection conn = ConnectDB.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int conversationId = -1;

                try (PreparedStatement pst
                        = conn.prepareStatement(findSql)) {

                    pst.setInt(1, userId1);
                    pst.setInt(2, userId2);

                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            conversationId
                                    = rs.getInt("ConversationID");
                        }
                    }
                }

                if (conversationId != -1) {
                    conn.commit();
                    return conversationId;
                }

                String insertConversationSql
                        = "INSERT INTO Conversations "
                        + "(ConversationType, CreatedBy) "
                        + "VALUES (?, ?)";

                try (PreparedStatement pst = conn.prepareStatement(
                        insertConversationSql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {

                    pst.setString(1, "private");
                    pst.setInt(2, userId1);

                    pst.executeUpdate();

                    try (ResultSet rs = pst.getGeneratedKeys()) {
                        if (rs.next()) {
                            conversationId = rs.getInt(1);
                        } else {
                            throw new java.sql.SQLException(
                                    "Không lấy được ConversationID!");
                        }
                    }
                }

                String insertMemberSql
                        = "INSERT INTO ConversationMembers "
                        + "(ConversationID, UserID) VALUES (?, ?)";

                try (PreparedStatement pst
                        = conn.prepareStatement(insertMemberSql)) {

                    pst.setInt(1, conversationId);
                    pst.setInt(2, userId1);
                    pst.executeUpdate();

                    pst.setInt(1, conversationId);
                    pst.setInt(2, userId2);
                    pst.executeUpdate();
                }

                conn.commit();
                return conversationId;

            } catch (java.sql.SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTaiKhoan = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtTimKiem = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        listBanBe = new javax.swing.JList<>();
        lblNguoiDung = new javax.swing.JLabel();
        txtTinNhan = new javax.swing.JTextField();
        btnDangXuat = new javax.swing.JButton();
        btnGui1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtKhungChat = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel2.setText("TÌM KIẾM");

        jLabel3.setText("DANH SÁCH BẠN BÈ");

        txtTimKiem.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtTimKiemKeyReleased(evt);
            }
        });

        listBanBe.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listBanBeMouseClicked(evt);
            }
        });
        listBanBe.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listBanBeValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(listBanBe);

        btnDangXuat.setText("ĐĂNG XUẤT");
        btnDangXuat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDangXuatActionPerformed(evt);
            }
        });

        btnGui1.setText("Gửi");
        btnGui1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGui1ActionPerformed(evt);
            }
        });

        jScrollPane3.setViewportView(txtKhungChat);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtTimKiem, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblNguoiDung, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(txtTinNhan, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnGui1, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(24, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnDangXuat)
                                .addGap(18, 18, 18)
                                .addComponent(lblTaiKhoan, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnDangXuat)
                    .addComponent(lblTaiKhoan, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addComponent(lblNguoiDung, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtTinNhan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnGui1)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtTimKiem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(9, 9, 9)
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDangXuatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDangXuatActionPerformed
        int choice = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất?", "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            frmLogin form = new frmLogin();
            form.setLocationRelativeTo(null);
            form.setVisible(true);
            if (timerTinNhan != null) {
                timerTinNhan.stop();
            }
            this.dispose();
        }        // TODO add your handling code here:
    }//GEN-LAST:event_btnDangXuatActionPerformed

    private void listBanBeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listBanBeMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_listBanBeMouseClicked

    private void listBanBeValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listBanBeValueChanged

        if (!evt.getValueIsAdjusting()) {
            String selected = listBanBe.getSelectedValue();

            if (selected != null) {
                String[] parts = selected.split("\\|", 2);
                selectedUserId = Integer.parseInt(parts[0]);

                lblNguoiDung.setText("Đang trò chuyện với: " + parts[1]);

                noiDungTinNhanCu = "";
                txtKhungChat.setText("");

                loadLichSuTinNhan(selectedUserId);
            }
        }


    }//GEN-LAST:event_listBanBeValueChanged

    private void btnGui1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGui1ActionPerformed
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người nhận tin nhắn!");
            return;
        }
        String content = txtTinNhan.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung tin nhắn!");
            return;
        }
        try (Connection conn = ConnectDB.getConnection()) {
            int conversationId = getOrCreateConversationId(currentUserId, selectedUserId);
            String sql = "INSERT INTO Messages " + "(ConversationID, SenderID, MessageContent, SentAt) " + "VALUES (?, ?, ?, GETDATE())";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, conversationId);
                pst.setInt(2, currentUserId);
                pst.setString(3, content);
                pst.executeUpdate();
            }
            txtTinNhan.setText("");
            loadLichSuTinNhan(selectedUserId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi tin nhắn: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnGui1ActionPerformed

    private void txtTimKiemKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTimKiemKeyReleased

        timKiemNguoiDung(txtTimKiem.getText());

        // TODO add your handling code here:
    }//GEN-LAST:event_txtTimKiemKeyReleased

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(frmChat.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(frmChat.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(frmChat.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(frmChat.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new frmLogin().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDangXuat;
    private javax.swing.JButton btnGui1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblNguoiDung;
    private javax.swing.JLabel lblTaiKhoan;
    private javax.swing.JList<String> listBanBe;
    private javax.swing.JTextPane txtKhungChat;
    private javax.swing.JTextField txtTimKiem;
    private javax.swing.JTextField txtTinNhan;
    // End of variables declaration//GEN-END:variables
}
