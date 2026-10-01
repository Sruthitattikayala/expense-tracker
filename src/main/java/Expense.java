public class Expense{
    
        int amount = 0;
        String category = "";
        String description = "";
        String date = "";
        public void setAmount(int amount){
                    
                    if(amount>0){
                        this.amount=amount;
                    }
                    else{
                        System.out.println("Enter valid amount:");
                    }

                }
        public int getAmount(){
            return amount;
        }
        public void setCategory(String category){
                this.category=category;
        }
        public String getCategory(){
            return category;
        }

        public void setDescription(String description){
                this.description=description;
        }
        public String getDescription(){
            return description;
        }

        public void setDate(String date){
                this.date=date;
        }
        public String getDate(){
            return date;
        }
                
}