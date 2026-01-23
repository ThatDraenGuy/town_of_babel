// Some generated code...

namespace LibrarySystem
{
    // Base abstract class
    public abstract class LibraryItem
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string Title { get; set; } = "";
        public DateTime DateAdded { get; set; } = DateTime.Now;

        public abstract string GetDescription();

        // Virtual method with default implementation
        public virtual void UpdateLastAccessed()
        {
            Console.WriteLine($"Item {Id} was accessed at {DateTime.Now}");
        }

        protected bool ValidateItem()
        {
            // Placeholder validation logic
            return !string.IsNullOrEmpty(Title) && Title.Length > 0;
        }
    }

    // Derived class 1
    public class Book : LibraryItem
    {
        public string Author { get; set; } = "";
        public string ISBN { get; set; } = "";
        public int PageCount { get; set; }
        public List<string> Genres { get; set; } = new List<string>();

        public override string GetDescription()
        {
            return $"{Title} by {Author} ({PageCount} pages)";
        }

        public void AddGenre(string genre)
        {
            // Placeholder method for adding genre
            if (!Genres.Contains(genre))
            {
                Genres.Add(genre);
            }
        }

        public bool IsLongBook()
        {
            // Placeholder method
            return PageCount > 500;
        }

        public string GetCitation(string format)
        {
            // Placeholder method for generating citations
            switch (format.ToLower())
            {
                case "apa":
                    return $"{Author} ({DateAdded.Year}). {Title}.";
                case "mla":
                    return $"{Author}. {Title}. {DateAdded.Year}.";
                default:
                    return $"{Author} - {Title}";
            }
        }
    }

    // Derived class 2
    public class DVD : LibraryItem
    {
        public string Director { get; set; } = "";
        public int DurationMinutes { get; set; }
        public string[] Actors { get; set; } = Array.Empty<string>();
        public string Rating { get; set; } = "NR";

        public override string GetDescription()
        {
            return $"{Title} directed by {Director} ({DurationMinutes} min, {Rating})";
        }

        public override void UpdateLastAccessed()
        {
            base.UpdateLastAccessed();
            Console.WriteLine($"DVD '{Title}' was checked out");
        }

        public string[] GetCast()
        {
            // Placeholder method
            return Actors ?? Array.Empty<string>();
        }

        public bool IsFeatureLength()
        {
            // Placeholder method
            return DurationMinutes >= 60;
        }

        public string FormatDuration()
        {
            // Placeholder method
            int hours = DurationMinutes / 60;
            int minutes = DurationMinutes % 60;
            return $"{hours}h {minutes}m";
        }
    }

    // Interface
    public interface IReviewable
    {
        void AddReview(Review review);
        double CalculateAverageRating();
        List<Review> GetRecentReviews(int count);
    }

    // Another class implementing interface
    public class Magazine : LibraryItem, IReviewable
    {
        public string Publisher { get; set; } = "";
        public int IssueNumber { get; set; }
        public DateTime PublicationDate { get; set; }
        private List<Review> _reviews = new List<Review>();

        public override string GetDescription()
        {
            return $"{Title} - Issue #{IssueNumber}, {PublicationDate:MMMM yyyy}";
        }

        public void AddReview(Review review)
        {
            // Placeholder implementation
            _reviews.Add(review);
        }

        public double CalculateAverageRating()
        {
            // Placeholder implementation
            if (_reviews.Count == 0) return 0;
            return _reviews.Average(r => r.Rating);
        }

        public List<Review> GetRecentReviews(int count)
        {
            // Placeholder implementation
            return _reviews.Take(count).ToList();
        }

        public bool IsCurrentIssue()
        {
            // Placeholder method
            return PublicationDate.Month == DateTime.Now.Month &&
                   PublicationDate.Year == DateTime.Now.Year;
        }

        public string GetPublisherInfo()
        {
            // Placeholder method
            return $"Published by {Publisher}";
        }
    }

    // Supporting class
    public class Review
    {
        public string ReviewerName { get; set; } = "";
        public int Rating { get; set; } // 1-5
        public string Comment { get; set; } = "";
        public DateTime ReviewDate { get; set; } = DateTime.Now;

        public string GetFormattedReview()
        {
            // Placeholder method
            return $"{ReviewerName}: {Rating}/5 - {Comment}";
        }

        public bool IsPositiveReview()
        {
            // Placeholder method
            return Rating >= 4;
        }
    }

    // Static utility class
    public static class LibraryHelper
    {
        public static int TotalItemsCreated { get; private set; }

        static LibraryHelper()
        {
            TotalItemsCreated = 0;
        }

        public static void IncrementItemCount()
        {
            // Placeholder method
            TotalItemsCreated++;
        }

        public static string GenerateItemCode(string type)
        {
            // Placeholder method
            return $"{type.ToUpper()}-{DateTime.Now:yyyyMMdd}-{Guid.NewGuid().ToString().Substring(0, 8)}";
        }

        public static bool ValidateISBN(string isbn)
        {
            // Placeholder method - real validation would be more complex
            return !string.IsNullOrEmpty(isbn) && isbn.Length >= 10;
        }

        public static DateTime CalculateDueDate(DateTime checkoutDate, int days = 14)
        {
            // Placeholder method
            return checkoutDate.AddDays(days);
        }
    }
}
