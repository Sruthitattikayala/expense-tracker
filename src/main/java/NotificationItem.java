public class NotificationItem {
    private int id;
    private String notificationKey;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private String createdAt;

    public NotificationItem() {}

    public NotificationItem(int id, String notificationKey, String title, String message, String type, boolean isRead, String createdAt) {
        this.id = id;
        this.notificationKey = notificationKey;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNotificationKey() { return notificationKey; }
    public void setNotificationKey(String notificationKey) { this.notificationKey = notificationKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
