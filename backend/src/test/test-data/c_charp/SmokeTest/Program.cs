// Some generated code...
namespace LibrarySystem
{
    // Main program class
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Library Management System");
            Console.WriteLine("=========================\n");

            // Demonstrate creating objects
            Book book = new Book
            {
                Title = "The C# Programming Language",
                Author = "Anders Hejlsberg",
                PageCount = 850,
                ISBN = "978-0-321-15491-6"
            };

            book.AddGenre("Programming");
            book.AddGenre("Technology");

            Console.WriteLine($"Book: {book.GetDescription()}");
            Console.WriteLine($"Is long book: {book.IsLongBook()}");
            Console.WriteLine($"APA Citation: {book.GetCitation("APA")}\n");

            // DVD example
            DVD dvd = new DVD
            {
                Title = "Inception",
                Director = "Christopher Nolan",
                DurationMinutes = 148,
                Rating = "PG-13",
                Actors = new[] { "Leonardo DiCaprio", "Joseph Gordon-Levitt", "Ellen Page" }
            };

            Console.WriteLine($"DVD: {dvd.GetDescription()}");
            Console.WriteLine($"Feature length: {dvd.IsFeatureLength()}");
            Console.WriteLine($"Duration: {dvd.FormatDuration()}\n");

            // Magazine example
            Magazine magazine = new Magazine
            {
                Title = "C# Monthly",
                Publisher = "Microsoft Press",
                IssueNumber = 42,
                PublicationDate = new DateTime(2024, 3, 1)
            };

            Console.WriteLine($"Magazine: {magazine.GetDescription()}");
            Console.WriteLine($"Current issue: {magazine.IsCurrentIssue()}");
            Console.WriteLine($"Publisher: {magazine.GetPublisherInfo()}\n");

            // Review example
            Review review = new Review
            {
                ReviewerName = "John Doe",
                Rating = 5,
                Comment = "Excellent resource for C# developers"
            };

            magazine.AddReview(review);
            Console.WriteLine($"Review: {review.GetFormattedReview()}");
            Console.WriteLine($"Positive review: {review.IsPositiveReview()}\n");

            // Using static helper
            Console.WriteLine($"Generated code: {LibraryHelper.GenerateItemCode("BOOK")}");
            Console.WriteLine($"ISBN Valid: {LibraryHelper.ValidateISBN("978-0-321-15491-6")}");
            Console.WriteLine($"Due Date: {LibraryHelper.CalculateDueDate(DateTime.Now):d}\n");

            // Demonstrate polymorphism
            List<LibraryItem> items = new List<LibraryItem> { book, dvd, magazine };

            Console.WriteLine("All Library Items:");
            Console.WriteLine("------------------");
            foreach (var item in items)
            {
                Console.WriteLine($"- {item.GetDescription()}");
                item.UpdateLastAccessed();
            }

            // Test interface implementation
            IReviewable reviewableItem = magazine;
            Console.WriteLine($"\nAverage Rating: {reviewableItem.CalculateAverageRating():F1}");

            Console.WriteLine("\nPress any key to exit...");
            Console.ReadKey();
        }
    }

    // Additional service class
    public class LibraryService
    {
        private List<LibraryItem> _inventory = new List<LibraryItem>();

        public void AddItem(LibraryItem item)
        {
            // Placeholder method
            _inventory.Add(item);
            LibraryHelper.IncrementItemCount();
            Console.WriteLine($"Added item: {item.Title}");
        }

        public bool RemoveItem(string id)
        {
            // Placeholder method
            var item = _inventory.FirstOrDefault(i => i.Id == id);
            if (item != null)
            {
                _inventory.Remove(item);
                return true;
            }
            return false;
        }

        public List<LibraryItem> SearchByTitle(string searchTerm)
        {
            // Placeholder method
            return _inventory
                .Where(item => item.Title.Contains(searchTerm, StringComparison.OrdinalIgnoreCase))
                .ToList();
        }

        public List<Book> GetAllBooks()
        {
            // Placeholder method
            return _inventory.OfType<Book>().ToList();
        }

        public Dictionary<string, int> GetInventoryCountByType()
        {
            // Placeholder method
            return _inventory
                .GroupBy(item => item.GetType().Name)
                .ToDictionary(group => group.Key, group => group.Count());
        }

        public void GenerateReport(string reportType)
        {
            // Placeholder method
            switch (reportType.ToLower())
            {
                case "inventory":
                    Console.WriteLine("Generating inventory report...");
                    break;
                case "circulation":
                    Console.WriteLine("Generating circulation report...");
                    break;
                case "overdue":
                    Console.WriteLine("Generating overdue items report...");
                    break;
                default:
                    Console.WriteLine("Generating default report...");
                    break;
            }
        }
    }
}
